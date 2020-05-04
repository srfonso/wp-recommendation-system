# -*- coding: utf-8 -*-
import scrapy
import requests
import json
from crawler_app.settings import *

# Define your item pipelines here
#
# Don't forget to add your pipeline to the ITEM_PIPELINES setting
# See: https://docs.scrapy.org/en/latest/topics/item-pipeline.html


# Pipeline que envía cada item como petición HTTP POST al servidor REST API
class RestApiPipeline(object):
  REST_API_HOST = ""
  REST_API_POST_METHOD = ""


  def __init__(self):
    #Leemos la configuración del json
    f = open(CONF_JSON_PATH,'r')
    datastore = json.load(f)

    # Cogemos la REST API HOST (si el introducido en json acaba con / se lo quitamos, para evitar errores con el método)
    aux_raurl = datastore["rest_api_host"]
    if aux_raurl[-1] == "/":
      aux_raurl = aux_raurl[:-1]
    self.REST_API_HOST = aux_raurl

    self.REST_API_POST_METHOD = datastore["rest_api_create_post_method"]

    f.close()
    super().__init__()


  def process_item(self, item, spider):

    # Comprobamos si el item recibido cumple con todos los campos
    if item['path'] and item['host'] and item['title'] and item['author'] and item['content'] and item['categories'] and item['time']:
      # Pasamos item a un diccionario para enviarlo por post
      data = {
      'path': item['path'], 'host' : item['host'], 'title' : item['title'], 'author': item['author'], 'content' : item['content'], 'categories' : item['categories'], 'time': item['time']
      }
      
      # Enviamos la petición
      response = requests.post(self.REST_API_HOST + self.REST_API_POST_METHOD, data = data)

      return item

    else:
      # En caso de faltar un lo ignoramos
      raise DropItem("Some field is missing")



        
