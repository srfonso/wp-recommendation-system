# -*- coding: utf-8 -*-
from rest_framework.routers import Route, SimpleRouter


# - Router personalizado para conectar con lucene -
class RecommendationRouter(SimpleRouter):
  """
  Class: Definimos las rutas/metodos que va a tener el recomendador y
  además las vinculamos con una función (que le aportará la lógica) y 
  con un método HTTP

  Attributes:
    routes Todas las rutas disponibles que habrá
  """
  routes = [
    Route(
      url=r'^index/create$',
      mapping={'get': 'create'},
      name='{basename}-create',
      detail=False,
      initkwargs={'suffix': 'Create'}
    ),
    Route(
      url=r'^index/read$',
      mapping={'get': 'read'},
      name='{basename}-read',
      detail=False,
      initkwargs={'suffix': 'Read'}
    ),
    Route(
      url=r'^{prefix}/nopersonalizada$',
      mapping={'post': 'getNoPersonalizada'},
      name='{basename}-getNoPersonalizada',
      detail=False,
      initkwargs={'suffix': 'getNoPersonalizada'}
    ),
    Route(
      url=r'^{prefix}/personalizada$',
      mapping={'get': 'getPersonalizada'},
      name='{basename}-getPersonalizada',
      detail=False,
      initkwargs={'suffix': 'getPersonalizada'}
    ),
    Route(
      url=r'^{prefix}/train$',
      mapping={'get': 'train'},
      name='{basename}-train',
      detail=False,
      initkwargs={'suffix': 'train'}
    )
  ]