# -*- coding: utf-8 -*-
from django.conf import settings
import numpy as np
import heapq
import json
import requests

class Recommendation():
  """
  Class: Para crear, usar, almacenar y modificar la tabla de "valoraciones" de
  cada usario para cada post (estas valoraciones serán el número de veces que ha hecho clic en el post)

  Con esta tabla se harán cálculos de recomendación filtrado colaborativo

  Attributes:
    logTable    Donde se almacena la matriz de usuarios/posts con las valoraciones
    
    user_to_id  Donde se traduce el usuario con la posición en la matriz logTable
    id_to_user  Diccionsrio inverso
    
    post_to_id  Donde se traduce las url de los post con la posición en la matriz logTable
    id_to_post Diccionario inverso

    user_neighborhood Lista de diccionarios con las similitudes entre usuarios
    post_neighborhood Lista de diccionarios con las similitudes entre post
    CB_neighborhood Lista de diccionarios con las similirudes entre post basado en contenido 
  """
  
  def __init__(self, user_list, post_list):
    self.logTable = np.zeros((len(user_list), len(post_list)))

    # Rellenamos los diccionarios de traducción
    self.user_to_id = {self.normalize_user(name): idx for idx, name in enumerate(user_list)}
    self.id_to_user = {idx: self.normalize_user(name) for idx, name in enumerate(user_list)}

    self.post_to_id = {self.normalize_post(post): idx for idx, post in enumerate(post_list)}
    self.id_to_post = {idx: self.normalize_post(post) for idx, post in enumerate(post_list)}

    # Vecindario de similitudes (una lista de diccionarios), el idx es el user/post
    self.user_neighborhood = []
    self.post_neighborhood = []
    self.CB_neighborhood = []
    

  #######################################################################################
  ###                                                                                 ###
  ###                                MÉTODOS GET                                      ###
  ###                                                                                 ###
  #######################################################################################

  def getUserSim(self, u, v):
    if v not in self.user_neighborhood[u]:
      if u not in self.user_neighborhood[v]:
        return 0
      return self.user_neighborhood[v][u]

    return self.user_neighborhood[u][v]

  def getItemSim(self, i, j):
    if (i >= len(self.post_neighborhood)) or (j not in self.post_neighborhood[i]):
      if (j >= len(self.post_neighborhood)) or (i not in self.post_neighborhood[j]):
        return 0
      return self.post_neighborhood[j][i]

    return self.post_neighborhood[i][j]

  def getItemSimCB(self, i, j, offline=True):

    # Primero intentamos obtener offline 
    if offline:
      sim = self.getItemSimCBOffline(i,j)
      # Devuelve -1 cuando no existe i,j en CB_neighborhood (por tanto, hacemos la peti online)
      if sim != -1:
        return sim

    #Primero obtenemos los path (por si acaso hemos recibido ids)
    urli = i
    if isinstance(i,int):
      urli = self.id_to_post[i]

    urlj = j
    if isinstance(i,int):
      urlj = self.id_to_post[j]


    # Hacemos la petición
    data = "urli="+urli+"&urlj="+urlj
    headers = {"Content-Type": "application/x-www-form-urlencoded"}  
    url = "http://127.0.0.1:5000/index/sim"
    #url = normalize_url(settings.INDEX_IP, settings.INDEX_PORT, settings.INDEX_SIM_METHOD)

    try:
      response = requests.post(url=url, data=data, headers=headers)

      # Recogemos la similitud devuelta
      json_file = json.loads(response.content)
      if "sim" in json_file:
        return json_file["sim"]
      return 0

    except requests.exceptions.RequestException as e:
      return 0

  def getItemSimCBOffline(self, i, j):
    """
    Si no existe devolvemos -1 (porque si se detecta -1 se hace una peticion online)
    """
    if (i >= len(self.CB_neighborhood)) or (j not in self.CB_neighborhood[i]):
      if (j >= len(self.CB_neighborhood)) or (i not in self.CB_neighborhood[j]):
        return -1
      return self.CB_neighborhood[j][i]

    return self.CB_neighborhood[i][j]

  #######################################################################################
  ###                                                                                 ###
  ###     FUNCIONES PARA CREAR LAS HERRAMIENTAS CON LAS QUE SE VA A RECOMENDAR        ###
  ###                                                                                 ###
  #######################################################################################


  def create(self, log_list):
    """
    Crea la tabla de valoraciones a partir de log_list

    Arguments:
      log_list Es una lista de diccionarios con las keys "user" y "dst"
    """
    for d in log_list:
      self.add(d["user"], d["dst"])

    # Actualizamos el diccionario de similitudes
    self.create_neighborhood()


  def add_and_refresh(self, user, post_visited):

    # Actualizamos la tabla
    self.add(user, post_visited)

    # Actualizamos el diccionario de similitudes
    self.create_neighborhood()


  def add(self, user, post_visited):
    """
    Modifica la tabla de valoraciones para aniadir un nuevo usuario

    [Sin actualizar el diccionario de similitud entre vecinos]

    Arguments:
      user Usuario a modififcar/aniadir
      post_visited Post visitado por el usuario
    """

    user = self.normalize_user(user)
    post_visited = self.normalize_post(post_visited)

    #Comprobamos si el usario ya está insertado en la tabla
    if user not in self.user_to_id:
      #Si no está lo aniadimos al final 
      useridx = len(self.user_to_id)
      self.user_to_id[user] = useridx
      self.id_to_user[useridx] = user

      # Y aniadimos una fila a la tabla
      self.logTable = np.vstack((self.logTable, np.zeros(m.shape[1])))

    else:
      useridx = self.user_to_id[user]


    #Comprobamos si el post ya está insertado en la tabla
    if post_visited not in self.post_to_id:
      #Si no está lo aniadimos al final 
      postidx = len(self.post_to_id)
      self.post_to_id[post_visited] = postidx
      self.id_to_post[postidx] = post_visited

      # Y aniadimos una fila a la tabla
      aux = np.zeros((self.logTable.shape[0], self.logTable.shape[1]+1))
      aux[:,:-1] = self.logTable
      self.logTable = aux

    else:
      postidx = self.post_to_id[post_visited]


    # Insertamos la tupla (numero de veces que user ha vistado post)
    self.logTable[useridx, postidx] += 1


  def create_neighborhood(self):
    """
    Crea el vecindario de similitudes
    """

    # Creamos el diccionario de users
    for idx in range(self.logTable.shape[0]): 
      self.create_user_neighborhood(idx)

    # Creamos el diccionario de posts (como es simétrica podemos obviar la mitad)
    list_j = list(range(self.logTable.shape[1]))
    for idx in range(self.logTable.shape[1]): 
      list_j.remove(idx) #Tabla simetrica
      self.create_post_neighborhood(idx, list_j)
      self.create_CB_neighborhood(idx, list_j)


  def create_user_neighborhood(self, user):
    """
    Crea el diccionario de similitudes del post

    Arguments:
      user Puede ser el string o el id

    Return:
      Diccionario de similitudes
    """

    #Obtenemos el id del post (podemos recibir o un string o id)
    userid = user
    if isinstance(user, str):
      if user not in self.user_to_id:
        return {}
      else:
        userid = self.user_to_id[user]
    
    # Creamos el diccionario de similitudes
    dic_aux = {}
    for idx in range(self.logTable.shape[0]):
      if idx != userid: # Nos saltamos a nosotros mismos
        dic_aux[idx] = self.CosineSimilarity(self.logTable[userid, :], self.logTable[idx, :])

    # Lo insertamos en la lista
    if userid == len(self.user_neighborhood):
      # Si userid es el siguiente indice de la lista, hacemos append
      self.user_neighborhood.append(dic_aux)
    elif userid < len(self.user_neighborhood):
      # Si ya está el indice, lo actualizamos
      self.user_neighborhood[userid] = dic_aux

    # Si corresponde a un indice muy fuera de rango se devuelve y que se use fuera
    return dic_aux


  def create_post_neighborhood(self, post, list_j=False):
    """
    Crea el diccionario de similitudes del post (basado en similitud coseno)

    Arguments:
      user Puede ser el string o el id
      list_j None=Recorre todos los post, sino, usa esa lista (permite saltarse post)

    Return:
      Diccionario de similitudes
    """

    #Obtenemos el id del user (podemos recibir o un string o id)
    postid = post
    if isinstance(post, str):
      if post not in self.post_to_id:
        return {}
      else:
        postid = self.post_to_id[post]
    
    # Usar todos los post o una lista personalizada pasa por argumento
    if isinstance(list_j, bool): # Si no hemos recibido una lista => False == Bool
      list_j = list(range(self.logTable.shape[1]))

    # Creamos el diccionario de similitudes (como es simetrica podemos saltarnos la mitad)
    dic_aux = {}
    for j in list_j:
      if j != postid: # Nos saltamos a nosotros mismos
        dic_aux[j] = self.CosineSimilarity(self.logTable[:, postid], self.logTable[:, j])

    # Lo insertamos en la lista
    if postid == len(self.post_neighborhood):
      # Si postid es el siguiente indice de la lista, hacemos append
      self.post_neighborhood.append(dic_aux)
    elif postid < len(self.post_neighborhood):
      # Si ya está el indice, lo actualizamos
      self.post_neighborhood[postid] = dic_aux

    # Si corresponde a un indice muy fuera de rango se devuelve y que se use fuera
    return dic_aux


  def create_CB_neighborhood(self, post, list_j=False):
    """
    Crea el diccionario de similitudes del post (basado en contenido)

    Arguments:
      user Puede ser el string o el id
      list_j None=Recorre todos los post, sino, usa esa lista (permite saltarse post)

    Return:
      Diccionario de similitudes
    """
    #Obtenemos el id del user (podemos recibir o un string o id)
    postid = post
    if isinstance(post, str):
      if post not in self.post_to_id:
        return {}
      else:
        postid = self.post_to_id[post]
    
    # Usar todos los post o una lista personalizada pasa por argumento
    if isinstance(list_j, bool): # Si no hemos recibido una lista => False == Bool
      list_j = list(range(self.logTable.shape[1]))

    # Creamos el diccionario de similitudes (como es simetrica podemos saltarnos la mitad)
    dic_aux = {}
    for j in list_j:
      if j != postid: # Nos saltamos a nosotros mismos
        dic_aux[j] = self.getItemSimCB(postid, j, offline= False)
        #print(dic_aux[j])

    # Lo insertamos en la lista
    if postid == len(self.CB_neighborhood):
      # Si postid es el siguiente indice de la lista, hacemos append
      self.CB_neighborhood.append(dic_aux)
    elif postid < len(self.CB_neighborhood):
      # Si ya está el indice, lo actualizamos
      self.CB_neighborhood[postid] = dic_aux

    # Si corresponde a un indice muy fuera de rango se devuelve y que se use fuera
    return dic_aux



  #######################################################################################
  ###                                                                                 ###
  ###                           FUNCIONES PARA RECOMENDAR                             ###
  ###            FC-(BASADA EN USUARIO, BASADA EN ITEMS)    BC-(KNN)                  ###
  #######################################################################################


  def getRatingCutOff(self, user, cutoff, func_rating=None, content_based=False):
    """
    Rating de filtrado colaborativo basado en contenido
    Calcula el rating r(u,i) del user para todos los items y devuelve el top cutoff
    
    Arguments:
      user    Usuario a obtener el top
      cutoff  Número de elementos a devolver
      func_rating Que funcion se rating se va a usar (User based o Item based)

    Return:
      String  Devuelve un json de tipo string 
    """
    if not func_rating:
      return "[]"

    # Recorremos todos los post (da igual post_to_id o id_to_post)
    ret_sim = []
    for post, idx in self.post_to_id.items():
      ret_sim.append((func_rating(user,post,content_based), post))

    # Ordenamos y cogemos el cutoff y formamos el 'json' de return
    json_ret = []
    i = 1
    for sim, post in self.heapsort(ret_sim):
      json_ret.append({'pos':i, 'doc':post}) # Ocultamos informacion extra innecesaria  'rating':sim})
      #json_ret.append({'pos':i, 'doc':post,'rating':sim})

      i+=1

    #print(json_ret[:cutoff])
    return json.dumps(json_ret[:cutoff])


  def getRatingUserBasedCutOff(self, user, cutoff):
    """
    Rating de filtrado colaborativo basado en contenido
    Calcula el rating r(u,i) del user para todos los items y devuelve el top cutoff
    
    Arguments:
      user    Usuario a obtener el top
      cutoff  Número de elementos a devolver

    Return:
      String  Devuelve un json de tipo string 
    """
    return self.getRatingCutOff(user, cutoff, func_rating=self.getRatingUserBased)


  def getRatingItemBasedCutOff(self, user, cutoff, content_based=False):
    """
    Rating de filtrado colaborativo basado en contenido
    Calcula el rating r(u,i) del user para todos los items y devuelve el top cutoff
    
    Arguments:
      user    Usuario a obtener el top
      cutoff  Número de elementos a devolver

    Return:
      String  Devuelve un json de tipo string 
    """
    return self.getRatingCutOff(user, cutoff, func_rating=self.getRatingItemBased, content_based=content_based)


  def getRatingUserBased(self, user, post, content_based=False):
    """
    Devuelve el Rating basado en usuarios normalizado rub(u,i) - Filtrado colaborativo
    Aunque este normalizado los ratings no son valores entre [0-1]. Eso son las similitudes.
  
    Arguments:
      user  String
      posts String

    Return:
      Double  Rating

    """
    # Obtenemos los id
    if (user not in self.user_to_id) or (post not in self.post_to_id):
      return 0
    
    userid = self.user_to_id[user]
    postid = self.post_to_id[post]

    # Recorremos cada vecino de user para calcular sim(u,v)*r(v,post)
    rating = 0
    for v in range(self.logTable.shape[0]):
      if userid != v:
        rating += (self.user_neighborhood[userid][v] * self.logTable[v, postid])

    # Obtenemos C para normalizar   c = (1 / E sim(u,v))
    c_aux = sum(self.user_neighborhood[userid].values(), 0.0)
    if c_aux == 0:
      return 0
    c = (1/c_aux)

    return (c*rating)


  def getRatingItemBased(self, user, post, content_based=False):
    """
    Devuelve el Rating basado en items normalizado rub(u,i) - Filtrado colaborativo
    Aunque este normalizado los ratings no son valores entre [0-1]. Eso son las similitudes.
  
    Arguments:
      user  String
      posts String

    Return:
      Double  Rating

    """

    # Obtenemos la funciona a usar
    sim_func = self.getItemSim
    if content_based:
      sim_func = self.getItemSimCB

    # Obtenemos los id
    if (user not in self.user_to_id) or (post not in self.post_to_id):
      return 0
    
    userid = self.user_to_id[user]
    postid = self.post_to_id[post]

    # Recorremos cada item para calcular sim(i,j)*r(u,j)
    rating = 0
    c_aux = 0
    for j in range(self.logTable.shape[1]):
      if postid != j:
        # Depdende de si es FC o BC obtenemos la similitud de una forma u otra
        sim = sim_func(postid, j)
        rating += ( sim * self.logTable[userid, j])
        c_aux += sim

    # Obtenemos C para normalizar   c = (1 / E sim(u,v))
    if c_aux == 0:
      return 0
    c = (1/c_aux)

    return (c*rating)




  #######################################################################################
  ###                                                                                 ###
  ###                                HERRAMIENTAS                                     ###
  ###          (Similitud entre vectores, HeapSort para ordenar lista, ...)           ###
  #######################################################################################


  def CosineSimilarity(self, vector_u, vector_v):
    """
    Similitud filtrado colaborativo
    Calcula la similitud coseno de dos vectores numpy

    Arguments:
      vector_u  Vector numpy
      vector_v  Vector numpy
 
    Return:
      Double  Similarity
    """

    # Producto escalar:
    p_e = np.dot(vector_u, vector_v)

    # Producto de modulos:
    mod = np.linalg.norm(vector_u)*np.linalg.norm(vector_v)
    if mod == 0:
      return 0

    return p_e/mod


  def heapsort(self, iterable, ascending=False):
    """
    Ordena un iterable (puede estar formado por pares (1, "hola"))
    
    Arguments: 
      ascending Orden ascendente si está a true

    Return:
      List  Lista ordenada
    """

    h = []
    for v in iterable:
      heapq.heappush(h,v)

    ret = [heapq.heappop(h) for i in range(len(h))]
    if ascending:
      return ret
    return list(reversed(ret))



  def normalize_post(self,post):
    aux = post.replace(" ", "").lower()

    aux = aux if aux.startswith("/") else "/" + aux
    aux = aux if aux.endswith("/") else aux + "/" 

    return aux 


  def normalize_user(self, user):
    return user.replace(" ","").lower()



#######################################################################################
###                                                                                 ###
###                    Normalizar la peticion a la API de Lucene.                   ###
###        La dejamos fuera para que la use tambien las peticiones en view.py       ###
#######################################################################################

def normalize_url(ip, port, method, secure=False):
  """
  Forma la url correctamente, permite evitar problemas si se ha olvidado 
  algun / o si hay de más.
  Ej 127.0.0.1:5000/index/create

  Arguments:
    ip 
    port
    method
    secure Para saber si aniadir http or https

  Return: String con la url formada
  """
  aux_m = method if method.startswith("/") else ("/" + method)
  aux_m = aux_m if aux_m.endswith("/") else (aux_m + "/")

  url = ip + ":" + str(port) + aux_m

  if secure:
    return "https://" + url
  return "http://" + url