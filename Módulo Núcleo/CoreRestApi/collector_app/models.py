# -*- coding: utf-8 -*-

from __future__ import unicode_literals
from django.db import models

# Modelos creados

class Log(models.Model):
    """
    Class: Crea entradas para cada uno de los logs recibidos al servidor.

    Attributes:
        user El identificador de usuario 
        ip  La ip del usuario
        src  URL origen (de donde proviene el click)
        dst  URL dst (hacia donde apunta el click)
        host  El host
        timestp Fecha de creación de la entrada log
    """
    
    user = models.CharField(max_length=256)
    ip = models.GenericIPAddressField(default='1.0.0.1')
    src = models.CharField(max_length=256)
    dst = models.CharField(max_length=256)
    host = models.CharField(max_length=32)
    timestp = models.DateTimeField(auto_now_add=True)


    # Metodo que se llama cuando se va a crear una nueva instancia
    # Podemos sobrescribirlos y añadirle lo que queramos.
    #def save(self, *args, **kwargs):
    #    self.catSlug = slugify(self.catName)
    #    super(Category, self).save(*args, **kwargs) # Llamamos al save del super.

    class Meta:
        verbose_name_plural = 'Logs'

    def __unicode__(self): # For Python 2, use __unicode__ too
        return self.user + "-" + self.ip + " to " + self.dst
    
    def __str__(self): # For Python 3
        return self.user + "-" + self.ip + " to " + self.dst









class Post(models.Model):
    """
    Class: Crea entradas para cada uno de los post recibidos del servidor.

    Attributes:
        path El identificador del post (junto con el host)
        host El host del post
        title Titulo
        author Creador del post
        content  El contenido del post
        categories Las categorias del post
        host  El host
        timestp Fecha de creación de la entrada log
    """
    
    path = models.CharField(max_length=512)
    host = models.CharField(max_length=64)
    title = models.CharField(max_length=128)
    author = models.CharField(max_length=64)
    content = models.CharField(max_length=81920)
    categories = models.CharField(max_length=256)
    time = models.CharField(max_length=256)


    # Metodo que se llama cuando se va a crear una nueva instancia
    # Podemos sobrescribirlos y añadirle lo que queramos.
    #def save(self, *args, **kwargs):
    #    self.catSlug = slugify(self.catName)
    #    super(Category, self).save(*args, **kwargs) # Llamamos al save del super.

    class Meta:
        verbose_name_plural = 'Posts'

        #Hacemos que la primary_key sea la url (path + host)
        unique_together = (('path', 'host'))

    def __unicode__(self): # For Python 2, use __unicode__ too
        return self.host + self.path
    
    def __str__(self): # For Python 3
        return self.host + self.path