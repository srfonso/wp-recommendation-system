<?php
/*
Plugin Name: recommendation_plugin
Plugin URI: 
Description: Plugin que actúa de frontend para el sistema de recomendación. Aporta información de los usuarios, solicita y muestra recomendaciones.
Version: 1.0.0
Author: Alfonso de Paz
Author URI: 
License: 
License URI: 
*/


/******************************************************************************
 **																			 **
 **	  					VARIABLES DE CONFIGURACIÓN   				    	 **
 **			(IP del servidor de recomendacion, metodos de este...)		     **
 ******************************************************************************/
$RECOMENDATION_SERVER_IP = 'http://127.0.0.1:8000/';
$LOG_METHOD = 'api/logs/';
$REC_NOTPERS_METHOD = 'api/recommendation/nopersonalizada';
$REC_PERS_METHOD = 'api/recommendation/personalizada';


/******************************************************************************
 **																			 **
 **	  ENVIAR LOS MOVIMIENTOS DE LOS USUARIOS AL SISTEMA DE RECOMENDACIÓN     **
 **																		     **
 ******************************************************************************/

// Una ves se ha cargado el objeto wp (la página) obtenemos el log del usuario
add_action( 'wp_head', 'js_injection' );

// Nos enganchamos al hook que llamará el admin-ajax.php (su hook es wp_ajax_+action)
add_action('wp_ajax_create_log_entry','create_log_entry');
add_action('wp_ajax_nopriv_create_log_entry','create_log_entry');



// Función que crea el log del servidor
function create_log_entry(){

	/* -- CAPA DE SEGURIDAD -- */
	// nonce check for an extra layer of security, the function will exit if it fails
	if ( !wp_verify_nonce( $_REQUEST['nonce'], "random_string")) {
	  $log = date("D M j G:i:s T Y")." => Security error (nonce couldn't be verify)";
	  file_put_contents('./logs/log_'.date("j.n.Y").'.log', $log, FILE_APPEND);
	  wp_die("Error - Verificación nonce no válida ✋");
	}   


	/* Primero si es una conexion comprartida*/
	if(!empty($_SERVER['HTTP_CLIENT_IP'])){

		$ip = $_SERVER['HTTP_CLIENT_IP'];

	/*Enn otro caso si ha pasado por un proxy*/
	}else if(!empty($_SERVER['HTTP_X_FORWARDED_FOR'])){
		# Devuelve una lista (separada por espacios o comas) de todas las ips de los proxies y cliente
		$ip = $_SERVER['HTTP_X_FORWARDED_FOR'];

	/* Sino es comprartida ni ha pasado por un proxy -> la cogemos directamente*/
	}else if(!empty($_SERVER['REMOTE_ADDR'])){
		
		$ip = $_SERVER['REMOTE_ADDR'];
	}


	//Si es localhost pues lo ponemos
	if($ip  == '::1'){
		$ip = '127.0.0.1';
	}

	// - Obtenemos SOURCE - Comprobamos si  proviene de ninguna web 
	if(empty($_REQUEST['src'])){
		$src_url = 'empty';
	}else{
		#$src_url = $_SERVER['HTTP_REFERER'];
		$src_url = parse_url($_REQUEST['src'], PHP_URL_PATH);
	}

	// - Obtenemos USER -
	$user = "Not logged";
	foreach ($_COOKIE as $key=>$val){
		if(strpos($key,"wordpress_logged_in_") !== false){
			$user = explode("|", $val)[0];
		}
	}

	$log = date("D M j G:i:s T Y")." => ";

	if(_isCurl() == false){
		// Si no existe curl lo indicamos en el log
		$log .= "Error, curl function not defined.";

	}else{

		# Enviamos la petición HTTP (ip - user - src - dst - host) y obtenemos la respuesta 
		$log .= send_http_post($GLOBALS["RECOMENDATION_SERVER_IP"].$GLOBALS["LOG_METHOD"], $ip, $user, $src_url,  $_REQUEST['dst'], $_SERVER['HTTP_HOST']) . "\n";
		#$log = "{$ip} {$_SERVER['REQUEST_URI']} {$_SERVER['HTTP_REFERER']}\n";
	}


	if(!file_exists("./logs/")){
		mkdir("./logs/", 0777, true);
	}


	# DEBUG
	#$log = "\n->".$user;
	/*foreach ($_COOKIE as $key=>$val){
		$log .= $key.' is '.$val."\n";
	}*/
	#$log = "_>".$_SERVER['SERVER_NAME']."-".$_SERVER['SERVER_ADDR']."-".$_SERVER['HTTP_HOST']."-".$_SERVER['REMOTE_HOST']."-".$_SERVER['PATH_TRANSLATED']."-".$_SERVER['REQUEST_URI']."-".$_SERVER['PATH_INFO']."\n";
	file_put_contents('./logs/log_'.date("j.n.Y").'.log', $log, FILE_APPEND);


	// don't forget to end your scripts with a die() function - very important
   	die();

}





# Función para enviar la petición HTTP usando post | USANDO curl
function send_http_post($url,$ip,$user,$src_url,$dst_url, $host){
	//$url = 'http://192.168.47.1/api/logs';

	$data = array('ip' => $ip, 'user' => $user, 'src' => $src_url, 'dst' => $dst_url, 'host' => $host);
	$data_in_post = http_build_query($data); //Generar una cadena de consulta codificada estilo URL arg1=1&arg2=2...

	//return $data_in_post;

	# Creamos la petición usando CURL
	$ch = curl_init();
	curl_setopt($ch, CURLOPT_URL, $url);
	
	// Aniaidmos los datos del post (el 1 = True)
	curl_setopt($ch, CURLOPT_POSTFIELDS, $data_in_post);

	curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
	curl_setopt($ch, CURLOPT_VERBOSE, 1);
	curl_setopt($ch, CURLOPT_HEADER, 1);
	curl_setopt($ch,CURLOPT_TIMEOUT_MS,200);


	# Enviamos la petición HTTP POST (context) a la url en concreto (URL) y obtenemos la respuesta
	$response = curl_exec($ch);

	//$header_size = curl_getinfo($ch, CURLINFO_HEADER_SIZE);
	//$header = substr($response, 0, $header_size);
	//$body = substr($response, $header_size);

	// Devolvemos el codigo de error HTTP EJ: HTTP/1.1 201 Created
	$header = strtok($response, "\n");

	curl_close($ch);

	return $header;
}

# Checkear si existe la función curl en la distribución PHP actual
function _isCurl(){
    return function_exists('curl_version');
}



function js_injection(){
	

	// Creamos un nonce para mayor seguridad
	$nonce = wp_create_nonce("random_string");

	// Obtenemos las páginas de source y dest

	// Comprobamos si  proviene de ninguna web 
	if(empty($_SERVER['HTTP_REFERER'])){
		$src_url = 'empty';
	}else{
		#$src_url = $_SERVER['HTTP_REFERER'];
		$src_url = parse_url($_SERVER['HTTP_REFERER'], PHP_URL_PATH);
	}

	// URL  Destino
	$dst_url = $_SERVER['REQUEST_URI'];


	/*
		-- Creamos el código javascript --
		
		Este código llamará a un php de wordpress, el cual crea y llama a un hook.

		A este hook nos hemos enganchado previamente y ejecuta la función create_log_entry().

	*/ 


	$link = "'".admin_url('admin-ajax.php?action=create_log_entry&src='.$src_url.'&dst='.$dst_url.'&nonce='.$nonce)."'";

	echo 

	"<script type='text/javascript'>

		//PHP call
		var http = new XMLHttpRequest();
		var url = ".$link.";
		http.open('GET', url, true);

		//Send the proper header information along with the request
		http.setRequestHeader('Content-type', 'application/x-www-form-urlencoded');

		http.send(null);
	
	</script>";

}

/*Creamos el shortcode*/
//add_shortcode('show_ip','create_log_entry');



/******************************************************************************
 **																			 **
 **	 			 MOSTRAR EL PLUGIN Y SOLICITAR RECOMENDACIONES  		     **
 **																		     **
 ******************************************************************************/

// Aniadir las localizaciones de nuestros widgets
function WidgetsLocationInit(){

	// Para todas las zonas posibles (no todos los temas tendran compatibilidad con todas las zonas)
	register_sidebar( array(
		'name' => 'Sidebar',
		'id' => 'sidebar1',
		'before_widget' => '<div class="widget-item',
		'after_widget' => '</div>'));

	register_sidebar( array(
		'name' => 'Footer Area 1',
		'id' => 'footer1',
		'before_widget' => '<div class="widget-item',
		'after_widget' => '</div>'));

	register_sidebar( array(
		'name' => 'Footer Area 2',
		'id' => 'footer2',
		'before_widget' => '<div class="widget-item',
		'after_widget' => '</div>'));

	register_sidebar( array(
		'name' => 'Footer Area 3',
		'id' => 'footer3',
		'before_widget' => '<div class="widget-item',
		'after_widget' => '</div>'));

	register_sidebar( array(
		'name' => 'Footer Area 4',
		'id' => 'footer4',
		'before_widget' => '<div class="widget-item',
		'after_widget' => '</div>'));
}


add_action('widget_init', 'WidgetsLocationInit');



function getRecomendationHTML($rec_type, $cutoff){

	// - Obtenemos USER -
	$user = "Not logged";
	foreach ($_COOKIE as $key=>$val){
		if(strpos($key,"wordpress_logged_in_") !== false){
			$user = explode("|", $val)[0];
		}
	}

	// Si el tipo es personalizada pero no esta logueado ningun usuarios:
	$random = FALSE;
	if ($rec_type == "pers" && $user == "Not logged"){
		// Cambiamos a nopers con,  el flag random a true, y el cutoff a 10
		$random = TRUE;
		$rec_type = "nopers";
		$cutoff = 10;
	}

	if ($rec_type == "pers"){
		$ret = getRecomendationPersonalized($user,$cutoff);
		$list =  "<div class='RP'><ul>";

	}else if($rec_type == "nopers"){
		$ret = getRecomendationNotPersonalized($_SERVER['REQUEST_URI'],$cutoff, $random);

		$list =  "<div class='RNP'><ul>";

	}else{
		$ret = array();
		$list = "<div><ul>";
	}

	foreach($ret as $el){
		$list .= "<li><a href=".$el['doc'].">".$el['title']."</a></li>";
	}
	$list .= "</ul></div>";


	return $list;
}


#add_shortcode('testhtml','getRecomendationHTML');

function getRecomendationPersonalized($user,$cutoff=5,$type=Null){

	$data = "?userid=".$user."&cutoff=".$cutoff;
	if(!is_null($type)){
		$data .= "&type=".$type;
	}	

	# Creamos la url con los argumentos
	$url = $GLOBALS["RECOMENDATION_SERVER_IP"].$GLOBALS["REC_PERS_METHOD"] . $data;
	# Creamos la petición usando CURL
	$ch = curl_init();
	curl_setopt($ch, CURLOPT_URL, $url);
	curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
	curl_setopt($ch,CURLOPT_TIMEOUT_MS,6000);

	# Enviamos la petición HTTP POST (context) a la url en concreto (URL) y obtenemos la respuesta
	$response = curl_exec($ch);
	$json = json_decode($response, TRUE);
	if(curl_errno($ch) || is_null($json)){
		return array();
	}

	if(curl_getinfo($ch, CURLINFO_HTTP_CODE) != 200){
		return array();
	} 

	# Ordenamos por el parámetro posicion
	usort($json, function($a, $b) { //Sort the array using a user defined function
    	return ((int)$a["pos"] < (int)$b["pos"]) ? -1 : 1; //Compare the position
	});  

	$ret = array();
	foreach ($json as $entry){
		if(isset($entry["doc"]) && isset($entry["title"])){
			array_push($ret, (array("doc" => $entry["doc"], "title" => $entry["title"])));
		}
	}

	return $ret;
}


function getRecomendationNotPersonalized($url, $cutoff, $random=FALSE){

	$data = array(
		'url' => $url,
		'cutoff' => $cutoff
	);
	$data_in_post = json_encode($data); //Generar un json

	# Creamos la url con los argumentos
	$url = $GLOBALS["RECOMENDATION_SERVER_IP"].$GLOBALS["REC_NOTPERS_METHOD"];

	# Creamos la petición usando CURL
	$ch = curl_init();
	curl_setopt($ch, CURLOPT_URL, $url);

	// Aniaidmos los datos del post (el 1 = True)
	curl_setopt($ch, CURLOPT_POSTFIELDS, $data_in_post);

	//set the content type to application/json
	curl_setopt($ch, CURLOPT_HTTPHEADER, array('Content-Type:application/json'));

	curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
	curl_setopt($ch,CURLOPT_TIMEOUT_MS,200);

	# Enviamos la petición HTTP POST (context) a la url en concreto (URL) y obtenemos la respuesta
	$response = curl_exec($ch);
	$json = json_decode($response, TRUE);
	if(curl_errno($ch) || is_null($json)){
		return array();
	}

	if(curl_getinfo($ch, CURLINFO_HTTP_CODE) != 200){
		return array();
	} 

	# Ordenamos por el parámetro posicion
	if($random){
		shuffle($json);
	}else{
		usort($json, function($a, $b) { //Sort the array using a user defined function
			return ((int)$a["pos"] < (int)$b["pos"]) ? -1 : 1; //Compare the position
		});  
	}

	$ret = array();
	foreach ($json as $entry){
		if(isset($entry["doc"]) && isset($entry["title"])){
			array_push($ret, (array("doc" => $entry["doc"], "title" => $entry["title"])));
		}
	}

	return $ret;
}


// Register and load the widget
function wpb_load_widget() {
    register_widget( 'recommendation_widget' );
}
add_action( 'widgets_init', 'wpb_load_widget' );
 
// Creating the widget 
class recommendation_widget extends WP_Widget {
 	

	function __construct() {
		parent::__construct(
		 
		// Base ID of your widget
		'recommendation_widget', 
		 
		// Widget name will appear in UI
		__('Recommendation Widget', 'recommendation_widget_domain'), 
		 
		// Widget description
		array( 'description' => __( 'Widget para situar las recomendaciones en la web', 'recommendation_widget_domain' ), ) 

		);

	}
	 
	// Creating widget front-end
	public function widget( $args, $instance ) {
		$title = apply_filters( 'widget_title', $instance['title'] );
		 
		// before and after widget arguments are defined by themes
		echo $args['before_widget'];
		if ( ! empty( $title ) )
		echo $args['before_title'] . $title . $args['after_title'];
		 
		// This is where you run the code and display the output
		echo __(getRecomendationHTML($instance['rec_type'], (int)$instance['rec_cutoff']), 'recommendation_widget_domain' );
		echo $args['after_widget'];
	}

	         
	// Widget Backend 
	public function form( $instance ) {
		if ( isset( $instance[ 'title' ] ) ) {
			$title = $instance[ 'title' ];
		}
		else {
			$title = __( 'New title', 'recommendation_widget_domain' );
		}

		// Tipo de recomendación
		if ( isset( $instance['rec_type']) && ($instance['rec_type'] == "nopers" || $instance['rec_type'] == "pers")){
			$rec_type = $instance['rec_type'];
		}else{
			$instance['rec_type'] = "nopers" ;
			$rec_type = "nopers" ;
		}

		// Cutoff 
		if ( isset( $instance['rec_cutoff']) && ($instance['rec_cutoff'] < 10 || $instance['rec_cutoff'] > 0)){
			$rec_cutoff = (int)$instance['rec_cutoff'];
		}else{
			$instance['rec_cutoff'] = 5 ;
			$rec_cutoff = 5;
		}

		// Widget admin form
		?>

		<p> 
			<label for="<?php echo $this->get_field_id( 'title' ); ?>"><?php _e( 'Title:' ); ?></label> 
			<input class="widefat" id="<?php echo $this->get_field_id( 'title' ); ?>" name="<?php echo $this->get_field_name( 'title' ); ?>" type="text" value="<?php echo esc_attr( $title ); ?>" />
			
			<br>
			<input name="<?php echo $this->get_field_name( 'rec_type' ); ?>" type="radio" id="<?php echo $this->get_field_id( 'rec_type' ); ?>" value="nopers" <?php echo ($rec_type== 'nopers') ?  "checked" : "" ;  ?>/> No personalizado 
			<input name="<?php echo $this->get_field_name( 'rec_type' ); ?>" type="radio" id="<?php echo $this->get_field_id( 'rec_type' ); ?>" value="pers" <?php echo ($rec_type== 'pers') ?  "checked" : "" ;  ?>/> Personalizado
			<br>
			<label for="<?php echo $this->get_field_id( 'rec_cutoff' ); ?>"><?php _e( 'Cutoff:' ); ?></label> 
			<input type="number" id="<?php echo $this->get_field_id( 'rec_cutoff' ); ?>" name="<?php echo $this->get_field_name( 'rec_cutoff' ); ?>" min="1" max="10" value="<?php echo esc_attr( $rec_cutoff ); ?>"/>
		</p>
		
		<?php 
	}
	     
	// Updating widget replacing old instances with new
	public function update( $new_instance, $old_instance ) {
		$instance = array();
		$instance['title'] = ( ! empty( $new_instance['title'] ) ) ? strip_tags( $new_instance['title'] ) : '';
		$instance['rec_type'] = ( ! empty( $new_instance['rec_type'] ) ) ? strip_tags( $new_instance['rec_type'] ) : '';
		$instance['rec_cutoff'] = ( ! empty( $new_instance['rec_cutoff'] ) ) ? strip_tags( $new_instance['rec_cutoff'] ) : 1;
		return $instance;
	}
} // Class recommendation_widget ends here