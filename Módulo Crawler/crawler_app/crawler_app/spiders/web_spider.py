# -*- coding: utf-8 -*-
import scrapy
import re
import json
import requests
from scrapy.linkextractors import LinkExtractor
from scrapy.spiders import CrawlSpider,Rule
from urllib.parse import urlparse
from crawler_app.items import *
from crawler_app.settings import *
from scrapy import signals
#from scrapy.xlib.pydispatch import dispatcher


class WebSpider(CrawlSpider):
  name = "WebCrawler"
  allowed_domains = []

  # Donde se encuentran las url donde comenxará el crawler
  start_urls = []

  # Localizacion de elementos en html
  author_xpath = ""
  cat_xpath = ""
  time_xpath = ""


  #Rules link to follow
  rules = {
    Rule(LinkExtractor(allow=(), allow_domains=allowed_domains, unique=True), callback='parse_item', follow=True),
  }


  def __init__(self):
    #Leemos la configuración del json
    f = open(CONF_JSON_PATH,'r')
    datastore = json.load(f)

    # Url donde comenzará el crawler
    self.start_urls.append(datastore["start_url_to_crawl"])
    self.allowed_domains.append(datastore["allowed_domain_to_crawl"])

    self.author_xpath = datastore["author_full_xpath"]
    self.cat_xpath = datastore["category_full_path"]
    self.time_xpath = datastore["time_full_xpath"]

    # Al iniciar el spider limpiamos los post aniadidos anteriomente
    api_host = datastore["rest_api_host"]
    if api_host[-1] == "/":
      api_host = api_host[:-1]
    method = datastore["rest_api_remove_post_method"]

    # Enviamos la petición (con seguridad habria que aniadir autenticacion)
    requests.delete(api_host + method)


    f.close()
    super().__init__()



  @classmethod
  def from_crawler(cls, crawler, *args, **kwargs):
    """
    Creamos un trigger que se active cuando finalice el crawler
    """
    spider = super(WebSpider, cls).from_crawler(crawler, *args, **kwargs)
    crawler.signals.connect(spider.on_quit, signal=signals.spider_closed)
    return spider


  def on_quit(self, spider,reason):
    """
    Cuando finalize el crawler, se llama a esta función que hace una petición al 
    módulo núcleo para que se cree el índice con los post que se acaban de obtener. 
    """
    
    #Leemos la configuración del json
    f = open(CONF_JSON_PATH,'r')
    datastore = json.load(f)
    
    # Hacemos la petición de crear el índice
    api_host = datastore["rest_api_host"]
    if api_host[-1] == "/":
      api_host = api_host[:-1]
    method = datastore["rest_api_create_index"]

    # Enviamos la petición (con seguridad habria que aniadir autenticacion)
    requests.get(api_host + method)

    f.close()



  def parse_item(self, response):
    post_item = PostItem()

    # ** Seccionamos la url para obtener los dos campos (Host y path)
    url_parsed = urlparse(response.request.url)
    post_item['path'] = url_parsed.path
    post_item['host'] = url_parsed.hostname


    # ** Vamos a usar XPATH para extraer elementos de la página obtenida ** 

    # El [1] es para coger el primero en caso de tener varios h1. (No debería haber)
    post_item['title'] = " ".join(response.xpath('normalize-space(//h1/text()[1])').extract())

    post_item['author'] = " ".join(response.xpath('normalize-space('+ self.author_xpath +'/text())').extract())

    # Intentamos obtener todo el texto entre etiquetas <p> (aunque también entrará contenido que no forma parte del post/noticia) 
    post_item['content'] = " ".join(response.xpath('normalize-space(//body/*/.)').extract())
    #post_item['content'] = listToString(re.findall(r'\w+', response.xpath('normalize-space(//p/text())').extract()))


    post_item['categories'] = " ".join(response.xpath('normalize-space('+ self.cat_xpath +'/text())').extract())

    #post_item['tags'] = ...

    post_item['time'] = " ".join(response.xpath('normalize-space('+ self.time_xpath +'/@datetime)').extract())


    yield post_item

    '''/*NEXT_PAGE_SELECTOR = '.next a ::attr(href)'
    next_page = response.css(NEXT_PAGE_SELECTOR).extract_first()
    if next_page:
        yield scrapy.Request(
            response.urljoin(next_page),
            callback=self.parse
        )'''







# Obtiene un string separado por espacios de una lista de palabras
# Function to convert   
def listToString(list):  

  # initialize an empty string 
  str1 = " " 

  # return string   
  return (str1.join(list)) 