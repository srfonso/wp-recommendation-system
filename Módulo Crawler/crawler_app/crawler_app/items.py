# -*- coding: utf-8 -*-

# Define here the models for your scraped items
#
# See documentation in:
# https://docs.scrapy.org/en/latest/topics/items.html

import scrapy


class PostItem(scrapy.Item):
  # define the fields for your item here like:
  path = scrapy.Field()
  host = scrapy.Field()
  title = scrapy.Field()
  author = scrapy.Field()
  content = scrapy.Field()
  categories = scrapy.Field()
  #tags = scrapy.Field()
  time = scrapy.Field()
