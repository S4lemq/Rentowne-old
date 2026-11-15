<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <xsl:import href="layout.xsl"/>
    <xsl:template name="document-content">
        <fo:block xsl:use-attribute-sets="section">ROZLICZENIE KAUCJI</fo:block>
        <xsl:call-template name="money-table"/>
        <xsl:for-each select="deductions/detail"><xsl:call-template name="detail"/></xsl:for-each>
        <xsl:call-template name="summary"/>
    </xsl:template>
</xsl:stylesheet>
