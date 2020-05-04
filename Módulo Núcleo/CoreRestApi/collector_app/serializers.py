# -*- coding: utf-8 -*-

#from django.contrib.auth.models import [MIS_MODELOS]
import copy
from rest_framework.validators import UniqueTogetherValidator
from rest_framework import serializers
from collector_app.models import *
#from django.core.validators import *

class LogSerializer(serializers.HyperlinkedModelSerializer):
  """
  Un simple serializer para leer modelos y pasarlos a json y viceversa.  
  """
  class Meta:
      model = Log
      fields = ['user', 'ip', 'src', 'dst', 'host', 'timestp']
      #fields = '__all__'



class PostSerializer(serializers.ModelSerializer):
  """
  Un simple serializer para leer modelos y pasarlos a json y viceversa.  
  """
  id = serializers.ReadOnlyField()

  class Meta:
    model = Post
    fields = ['id', 'path', 'host', 'title','author', 'content', 'categories', 'time']
    #fields = '__all__'

  def run_validators(self, value):
    for validator in copy.copy(self.validators):
      if isinstance(validator, UniqueTogetherValidator):
        self.validators.remove(validator)
    super(PostSerializer, self).run_validators(value)

  def create(self, validated_data):
    """
    Crea o actualiza (si existía) y devuelve una nueva instancia 'Post' con el validated_data dado.
    """
    post, created = Post.objects.update_or_create(
      # Comprobamos si existe ya una entrada con ese path y host
      path=validated_data['path'], host=validated_data['host'], 

      # Si existe, actualizamos los campos indicados a continuación
      defaults={
      'title': validated_data['title'],
      'author': validated_data['author'],
      'content': validated_data['content'],
      'categories': validated_data['categories'],
      'time': validated_data['time']
      })
    return post