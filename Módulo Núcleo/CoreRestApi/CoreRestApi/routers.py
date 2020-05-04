import collector_app.views as coll_view
import recommend_app.views as recm_view
import recommend_app.router as recm_router
from rest_framework import routers

"""
Router le permite declarar rápidamente todas las rutas comunes para un controlador (clase de view.py) dado. 
En lugar de declarar rutas separadas para su índice las declara en una sola línea de código.
"""


"""
  -- collector_app --
"""
router_collector = routers.DefaultRouter()
router_collector.register('logs', coll_view.LogViewSet, basename="logvs")
router_collector.register('posts', coll_view.PostViewSet, basename="postvs")

"""
  -- recommend_app --
"""
router_recommend = recm_router.RecommendationRouter()
router_recommend.register('recommendation', recm_view.RecommendationView, basename="recommendationvs")