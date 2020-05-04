# -*- coding: utf-8 -*-
from django.shortcuts import render
from django.conf import settings

from rest_framework import viewsets,status
from rest_framework.response import Response
from rest_framework.decorators import action

from collector_app.models import *
from recommend_app.models import *

import json
import requests # Enviar peticiones
from http.client import responses # Para obtener los mensajes de respuesta


# ATRIBUTO RECOMENDADOR (HAY QUE ENTRENAR (train) ANTES)
RECOMMENDER = None

# - Vistas creadas -

class RecommendationView(viewsets.ViewSet):
  """
  Class: View de la parte de recomendación, aquí se encuentra la funcionalidad
  de todos los métodos relacionados con recomendacion. 
  Este clase se conecta con el módulo del indice para crearlo, obtener recomendaciones ...
  
  """

  # Atributos 
  #recommender = None


  def create(self, request):
    return self.requestAPI(url=normalize_url(settings.INDEX_IP, settings.INDEX_PORT, settings.INDEX_CREATE_METHOD), error_message="Impossible to create the index.")


  def read(self, request):
    return self.requestAPI(url=normalize_url(settings.INDEX_IP, settings.INDEX_PORT, settings.INDEX_READ_METHOD), error_message="Impossible to read the index.")


  # /recommendation/nopersonalizada?userid=...&cutoff=...&type="ufc/ibc/ifc"
  def getPersonalizada(self, request):
    """
    Devuelve post para una recomendación personalizada
    
    Argumentos:

    requests Toda la información de la peticion
                - Argumentos GET:
                  userid Obligatorio - Usuario del que recomendar
                  cutoff Optativo - Numero de documentos máximos
                  type   Optativo - Especificar un tipo donde obtener la recomendacion: 
                                     - "ufc" Solo User Based de filtrado colaborativo
                                     - "ibc" Solo Item Based de Basado en contenido
                                     - "ifc" Solo Item Based de filtrado colaborativo
    """
    global RECOMMENDER

    if 'userid' not in request.query_params:
      return Response({
        'status': 'Bad request',
        'message': 'The \'userid\' param is needed.'
      }, status=status.HTTP_400_BAD_REQUEST)

    if not RECOMMENDER:
      return Response({
        'status': responses[status.HTTP_503_SERVICE_UNAVAILABLE],
        'message': 'Please, you need to train the model before recommending.' + str(RECOMMENDER) + "...."
      }, status=status.HTTP_503_SERVICE_UNAVAILABLE)

    user_id = request.query_params['userid']

    # Revisamos los argumentos opativos
    cutoff = 5
    if 'cutoff' in request.query_params:
      cutoff = int(request.query_params['cutoff'])

    ufc = True
    ibc = True
    ifc = True
    if 'type' in request.query_params:
      ufc = ibc = ifc = False
      if request.query_params['cutoff'] == 'ufc':
        ufc = True
      elif request.query_params['cutoff'] == 'ibc':
        ibc = True
      elif request.query_params['cutoff'] == 'ifc':
        ifc = True

    user_fc_json = item_bc_json = item_fc_json = []
    if ufc:
      user_fc_json = json.loads(RECOMMENDER.getRatingUserBasedCutOff(user_id, cutoff))
    if ibc:
      item_bc_json = json.loads(RECOMMENDER.getRatingItemBasedCutOff(user_id, cutoff, content_based=True))
    if ifc:
      item_fc_json = json.loads(RECOMMENDER.getRatingItemBasedCutOff(user_id, cutoff))

    # Vamos a ir cogiendo uno de cada lista hasta llenar el cupo (va en orden de prioridad)
    lista_ratings = [user_fc_json, item_bc_json, item_fc_json]
    # [Para evitar duplicados] - Indice de cada json, por si necesitamos saltar al siguiente en uno de los json (tener un indice individual) Round-Robin
    idx_ratings = [0,0,0]

    # Cogemos un elementos de cada lista hasta completar el cupo (cutoff)
    json_response = []
    i = 0;
    while i < cutoff:

      # i%len(lista_ratings) y asi segun avanzamos en el cutoff recorremos uno uno la lista
      if len(lista_ratings[i%len(lista_ratings)]) > 0: # Por si en algun recomender quedan/hay 0 resultados

        # [Para evitar duplicados] - Comprobamos si vamos a insertar un duplicado, en ese caso pasamos al siguiente elemento (para eso usamos los indices de cada json)
        if any(d.get('doc',None) == lista_ratings[i%len(lista_ratings)][idx_ratings[i%len(lista_ratings)]]['doc'] for d in json_response):
          idx_ratings[i%len(lista_ratings)] += 1 # Pasamos al siguiente elemento

          # No aumentamos i, por lo que se volverá a coger el mismo json el cual ahora apuntará al siguiente elemento

        else:
          json_response.append(lista_ratings[i%len(lista_ratings)][idx_ratings[i%len(lista_ratings)]])
          idx_ratings[i%len(lista_ratings)] += 1

          # Hay que editar la variable de posición
          json_response[len(json_response) - 1]['pos'] = len(json_response)

          # Aniaidmos el titulo (filtramos por el path y cogemos el titulo del primer resultado (solo debería haber un resultado en la lista))
          json_response[len(json_response) - 1] = self.dict_add_title(json_response[len(json_response) - 1])

          # Una vez insertado 1, aumentamos i
          i += 1

        # Ya no, tenemos indices para cada json -- del lista_ratings[i%len(lista_ratings)][0] # Borramos el elemento para que el siguiente avance (y no coger siempre el mismo)

      else:
        # Si lista_ratings[i%len(lista_ratings)] esta vacio, pasamos al siguiente
        i += 1


    # Enviamos la respuesta 
    #print(json_response)
    return Response(json_response, status=status.HTTP_200_OK)


  def getNoPersonalizada(self, request):

    if "url" not in request.data or "cutoff" not in request.data:
      return Response({
        'status': 'Bad request',
        'message': 'The \'url\' or \'cutoff\' param is needed.'
      }, status=status.HTTP_400_BAD_REQUEST)

    #Formato application/x-www-form-urlencoded
    data = "url=" + request.data["url"] + "&cutoff=" + str(request.data["cutoff"])
    headers = {"Content-Type": "application/x-www-form-urlencoded"}
    return self.requestAPI(url=normalize_url(settings.INDEX_IP, settings.INDEX_PORT, settings.INDEX_TOPSIM_METHOD), data=data, headers=headers, requests_call=requests.post, error_message="Impossible to get the similarities.", edit_json=self.json_add_title)


  def train(self,request):
    global RECOMMENDER

    # Obtenemos la lista de usuarios y la lista de posts únicos
    user_list = Log.objects.values_list('user', flat=True).distinct()
    post_list = Post.objects.values_list('path', flat=True).distinct()
   
    # Llamar inicializar el modelo Recommend() (coger de la lista el numero de usuarios únicos y post únicos)
    RECOMMENDER = Recommendation(user_list, post_list)

    # Coger todos los logs y tranformarlo en una lista de diccionarios {"user": ... , "post_visited": ...}
    # ** ELIMINAMOS TODOS LOS LOGS QUE NO CORRESPONDEN CON VISITAS A POSTS **
    log_list = Log.objects.filter(dst__in= post_list).values('user', 'dst')

    # DEBUG
    #for d in dict_list:
    #  print(d['user'],d['dst'])

    # Llamar a recommend.create()
    RECOMMENDER.create(log_list)
    #print(RECOMMENDER.getItemSimCB(0,1,offline=False))
    #print(RECOMMENDER.CB_neighborhood)

    # Enviamos la respuesta 
    return Response(status=status.HTTP_201_CREATED)


  def requestAPI(self, url, requests_call=requests.get, data=None, headers=None, error_message="", edit_json=None):
    """
    Envía una petición y devuelve la respuesta

    Arguments:
      url 
      requests_call (requests.get/requests.post)
      data
      error_message

    Return: Response (clase de django)
    """
    try:
      response = requests_call(url=url, data=data, headers=headers)

      # Comprobamos si recibimos un diccionario, sino devolvemos un template
      try:

        # Antes de modificar el json, comprobamos si hemos recibido una respuesta correcta o un bad request
        if response.status_code == 404:
          raise ValueError('La api ha devuelto' +  responses[response.status_code] + '. Saltamos para manejar el error.')

        dic_json = json.loads(response.content)

        if edit_json:
          dic_json = edit_json(dic_json)

      except ValueError:
        dic_json = {
        'status': responses[response.status_code],
        'message': response.content
        }

      return Response(dic_json, status=response.status_code)

    except requests.exceptions.RequestException as e: 
      return Response({
        'status': responses[status.HTTP_503_SERVICE_UNAVAILABLE],
        'message': error_message
      }, status=status.HTTP_503_SERVICE_UNAVAILABLE)



  #######################################################################################
  ###                                                                                 ###
  ###                     Edita un json recibido y le aniade title                    ###
  ###                                                                                 ###
  #######################################################################################

  def json_add_title(self,json):
    if not json:
      return None

    ret_json = []
    if isinstance(json,list):
      for dic in json:
        ret_json.append(self.dict_add_title(dic))
    elif isinstance(json,dict):
      ret_json = self.dict_add_title(json)

    return ret_json


  def dict_add_title(self,dic_):
    if not dic_ or ('doc' not in dic_):
      return dic_

    # Aniaidmos el titulo (filtramos por el path y cogemos el titulo del primer resultado (solo debería haber un resultado en la lista))
    queryset = Post.objects.filter(path__contains=dic_['doc']).values('title')
    if len(queryset) > 0: 
        dic_['title'] =  queryset[0]['title']

    return dic_

