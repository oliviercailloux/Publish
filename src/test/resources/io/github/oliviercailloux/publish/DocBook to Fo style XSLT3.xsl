<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
  xmlns:fo="http://www.w3.org/1999/XSL/Format" version="3.0">
  <xsl:param name="stylesheet.root"
    select="'https://cdn.docbook.org/release/xsl/1.79.2/fo/docbook.xsl'" static="yes" />
  <xsl:import _href="{$stylesheet.root}" />
  <xsl:param name="paper.type" select="'A4'" />
  <xsl:param name="fop1.extensions" select="1" />
  <xsl:param name="toc.section.depth" select="3" />
</xsl:stylesheet>