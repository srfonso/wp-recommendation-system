# -*- coding: utf-8 -*-
from django.shortcuts import render
from django.shortcuts import get_object_or_404

from rest_framework import viewsets,status
from rest_framework.response import Response
from rest_framework.decorators import action

from collector_app.models import *
from collector_app.serializers import *


#  - Vistas creadas -

class LogViewSet(viewsets.ViewSet):
  """
  Un simple ViewSet para listar, crear y recuperar logs 
  """

  def list(self, request):
    #Obtenemos todos los objetos
    queryset = Log.objects.all()

    #Los pasamos al serializer (que creará el json)
    serializer = LogSerializer(queryset, many=True)

    #Devolvemos una respuesta HTTP con el JSON
    return Response(serializer.data)


  def retrieve(self, request, pk=None):
    #Obtenemos todos los objetos
    queryset = Log.objects.all()

    # En retrieve los filtramos:
    log = get_object_or_404(queryset, pk=pk)

    #Los pasamos al serializer (que creará el json)
    serializer = LogSerializer(log)

    #Devolvemos una respuesta HTTP con el JSON
    return Response(serializer.data)


  def create(self,request):
    #Los pasamos al serializer la petición (en json) para aniadirla al modelo
    serializer = LogSerializer(data=request.data)

    if serializer.is_valid():
      serializer.save()

      # Devolvemos respuesta HTTP Ok con el dato json introducido
      return Response(serializer.data,status=status.HTTP_201_CREATED)

    #Devolvemos la respuesta HTTP de que no se ha podido crear el nuevo elemento
    return Response({
      'status': 'Bad request',
      'message': 'Account could not be created with received data.'
    }, status=status.HTTP_400_BAD_REQUEST)




class PostViewSet(viewsets.ViewSet):
  """
  Un simple ViewSet para listar, crear, actualizar y recuperar posts 
  """

  def list(self, request):
    #Obtenemos todos los objetos
    queryset = Post.objects.all()

    #Los pasamos al serializer (que creará el json)
    serializer = PostSerializer(queryset, many=True)

    #Devolvemos una respuesta HTTP con el JSON
    return Response(serializer.data)


  def retrieve(self, request, pk=None):
    #Obtenemos todos los objetos
    queryset = Post.objects.all()

    # En retrieve los filtramos:
    post = get_object_or_404(queryset, pk=pk)

    #Los pasamos al serializer (que creará el json)
    serializer = PostSerializer(post)

    #Devolvemos una respuesta HTTP con el JSON
    return Response(serializer.data)


  def create(self,request):
    #Los pasamos al serializer la petición (en json) para aniadirla al modelo
    serializer = PostSerializer(data=request.data)
    serializer.is_valid(raise_exception=True)
    if serializer.is_valid(raise_exception=True):
      serializer.save()

      # Devolvemos respuesta HTTP Ok con el dato json introducido
      return Response(serializer.data,status=status.HTTP_201_CREATED)

    #Devolvemos la respuesta HTTP de que no se ha podido crear el nuevo elemento
    return Response({
      'status': 'Bad request',
      'message': 'Account could not be created with received data.'
    }, status=status.HTTP_400_BAD_REQUEST)


  @action(methods=['delete'], detail=False)
  def remove(self, request, pk=None):
    instance = self.get_object()
    self.perform_destroy(instance)
    return Response(status=status.HTTP_204_NO_CONTENT)

  def perform_destroy(self, instance):
    instance.delete()

  def get_object(self):
    return Post.objects.all()