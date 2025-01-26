<?xml version='1.0'?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format" version="1.0">
	<!-- Could also import /usr/share/xml/docbook/stylesheet/docbook-xsl-ns/fo/docbook.xsl 
		from the Debian docbook-xsl-ns package. The CDN file was offline on 13th 
		of Sept 2021. -->
	<xsl:import href="https://cdn.docbook.org/release/xsl/1.79.2/fo/docbook.xsl" />
	<xsl:param name="paper.type" select="'A4'" />
	<xsl:param name="fop1.extensions" select="1" />
	<xsl:param name="toc.section.depth" select="3" />
</xsl:stylesheet>