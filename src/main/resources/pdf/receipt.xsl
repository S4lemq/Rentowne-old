<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <xsl:import href="layout.xsl"/>
    <xsl:template name="document-content">
        <fo:block xsl:use-attribute-sets="section">ZESTAWIENIE NALEŻNOŚCI</fo:block>
        <xsl:call-template name="money-table"><xsl:with-param name="numbered" select="true()"/></xsl:call-template>
        <xsl:call-template name="summary"/>
    </xsl:template>
</xsl:stylesheet>
