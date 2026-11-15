<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:fo="http://www.w3.org/1999/XSL/Format">
    <xsl:output method="xml" encoding="UTF-8"/>
    <xsl:attribute-set name="label">
        <xsl:attribute name="color">#667484</xsl:attribute>
        <xsl:attribute name="font-weight">bold</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="section">
        <xsl:attribute name="font-size">8pt</xsl:attribute>
        <xsl:attribute name="color">#144270</xsl:attribute>
        <xsl:attribute name="space-before">4mm</xsl:attribute>
        <xsl:attribute name="space-after">3mm</xsl:attribute>
        <xsl:attribute name="keep-with-next.within-page">always</xsl:attribute>
    </xsl:attribute-set>
    <xsl:attribute-set name="cell">
        <xsl:attribute name="padding">2mm 3mm</xsl:attribute>
        <xsl:attribute name="border-bottom">0.5pt solid #d8e0e8</xsl:attribute>
        <xsl:attribute name="display-align">center</xsl:attribute>
    </xsl:attribute-set>

    <xsl:template match="/document">
        <fo:root font-family="Inter" font-size="9.5pt" line-height="1.3" color="#1f2b3a">
            <fo:layout-master-set>
                <fo:simple-page-master master-name="a4" page-width="210mm" page-height="297mm"
                                       margin-top="14mm" margin-bottom="12mm" margin-left="15mm" margin-right="15mm">
                    <fo:region-body margin-top="35mm" margin-bottom="11mm"/>
                    <fo:region-before extent="33mm"/>
                    <fo:region-after extent="8mm"/>
                </fo:simple-page-master>
            </fo:layout-master-set>
            <fo:page-sequence id="document-pages" master-reference="a4" language="pl" country="PL" force-page-count="no-force">
                <fo:static-content flow-name="xsl-region-before">
                    <fo:table table-layout="fixed" width="100%">
                        <fo:table-column column-width="55%"/><fo:table-column column-width="45%"/>
                        <fo:table-body><fo:table-row>
                            <fo:table-cell><fo:block><fo:external-graphic src="url('classpath:/image/logo_rentowne.png')" content-width="48mm" content-height="scale-to-fit" scaling="uniform"/></fo:block></fo:table-cell>
                            <fo:table-cell><fo:block text-align="right" font-size="8pt" color="#667484">
                                <fo:block>Miejsce wystawienia: <xsl:value-of select="issueCity"/></fo:block>
                                <fo:block>Data wystawienia: <xsl:value-of select="issueDate"/></fo:block>
                            </fo:block></fo:table-cell>
                        </fo:table-row></fo:table-body>
                    </fo:table>
                    <fo:block font-size="18pt" font-weight="bold" color="#144270" space-before="2mm"><xsl:value-of select="heading"/></fo:block>
                    <fo:block font-weight="bold" color="#667484" space-before="1mm" padding-bottom="3mm" border-bottom="0.7pt solid #144270">NR <xsl:value-of select="number"/></fo:block>
                </fo:static-content>
                <fo:static-content flow-name="xsl-region-after">
                    <fo:block border-top="0.5pt solid #d8e0e8" padding-top="3mm" font-size="7.5pt" color="#667484">
                        <fo:table table-layout="fixed" width="100%">
                            <fo:table-column column-width="75%"/><fo:table-column column-width="25%"/>
                            <fo:table-body><fo:table-row>
                                <fo:table-cell><fo:block>RENTOWNE NIERUCHOMOŚCI | <xsl:value-of select="number"/></fo:block></fo:table-cell>
                                <fo:table-cell><fo:block text-align="right">Strona <fo:page-number/> z <fo:page-number-citation-last ref-id="document-pages"/></fo:block></fo:table-cell>
                            </fo:table-row></fo:table-body>
                        </fo:table>
                    </fo:block>
                </fo:static-content>
                <fo:flow flow-name="xsl-region-body">
                    <xsl:call-template name="parties"/>
                    <xsl:for-each select="details/detail"><xsl:call-template name="detail"/></xsl:for-each>
                    <xsl:call-template name="document-content"/>
                    <xsl:if test="payment/detail">
                        <fo:block keep-together.within-page="always" space-before="3mm">
                            <fo:block xsl:use-attribute-sets="section"><xsl:value-of select="payment/heading"/></fo:block>
                            <xsl:for-each select="payment/detail"><xsl:call-template name="detail"/></xsl:for-each>
                            <xsl:if test="payment/representativeAccount='true'"><fo:block font-size="7.5pt" color="#667484" space-before="3mm">Zapłata na rachunek bankowy pełnomocnika stanowi zapłatę na rzecz Wynajmującego.</fo:block></xsl:if>
                        </fo:block>
                    </xsl:if>
                    <xsl:if test="note!=''"><fo:block font-size="8pt" color="#667484" space-before="4mm" wrap-option="wrap"><xsl:value-of select="note"/></fo:block></xsl:if>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>

    <xsl:template name="parties">
        <fo:table table-layout="fixed" width="100%" border-bottom="0.5pt solid #d8e0e8" space-after="4mm" keep-together.within-page="always">
            <fo:table-column column-width="50%"/><fo:table-column column-width="50%"/>
            <fo:table-body><fo:table-row>
                <xsl:for-each select="parties/party">
                    <fo:table-cell padding-bottom="5mm" padding-right="6mm">
                        <fo:block font-size="7.5pt" color="#667484" space-after="2mm"><xsl:value-of select="@label"/></fo:block>
                        <fo:block font-weight="bold" wrap-option="wrap"><xsl:value-of select="name"/></fo:block>
                        <xsl:for-each select="address"><fo:block wrap-option="wrap"><xsl:value-of select="."/></fo:block></xsl:for-each>
                        <xsl:if test="representative!=''"><fo:block wrap-option="wrap">Pełnomocnik: <xsl:value-of select="representative"/></fo:block></xsl:if>
                    </fo:table-cell>
                </xsl:for-each>
            </fo:table-row></fo:table-body>
        </fo:table>
    </xsl:template>

    <xsl:template name="detail">
        <fo:table table-layout="fixed" width="100%" keep-together.within-page="always">
            <fo:table-column column-width="44mm"/><fo:table-column column-width="proportional-column-width(1)"/>
            <fo:table-body><fo:table-row>
                <fo:table-cell padding-bottom="1mm" padding-right="3mm"><fo:block xsl:use-attribute-sets="label"><xsl:value-of select="@label"/></fo:block></fo:table-cell>
                <fo:table-cell padding-bottom="1mm"><fo:block wrap-option="wrap"><xsl:value-of select="."/></fo:block></fo:table-cell>
            </fo:table-row></fo:table-body>
        </fo:table>
    </xsl:template>

    <xsl:template name="money-table">
        <xsl:param name="numbered" select="false()"/>
        <fo:table table-layout="fixed" width="100%" table-omit-header-at-break="false" space-after="3mm">
            <xsl:if test="$numbered"><fo:table-column column-width="11mm"/></xsl:if>
            <fo:table-column column-width="proportional-column-width(1)"/><fo:table-column column-width="44mm"/>
            <fo:table-header background-color="#144270" color="white" font-weight="bold">
                <fo:table-row>
                    <xsl:if test="$numbered"><fo:table-cell padding="2mm"><fo:block font-size="7.5pt">Lp.</fo:block></fo:table-cell></xsl:if>
                    <fo:table-cell padding="2mm 3mm"><fo:block>Nazwa pozycji</fo:block></fo:table-cell>
                    <fo:table-cell padding="2mm 3mm"><fo:block text-align="right">Kwota</fo:block></fo:table-cell>
                </fo:table-row>
            </fo:table-header>
            <fo:table-body>
                <xsl:for-each select="lines/line">
                    <fo:table-row keep-together.within-page="always">
                        <xsl:if test="$numbered"><fo:table-cell xsl:use-attribute-sets="cell" padding="2mm"><fo:block font-size="7.5pt" color="#667484"><xsl:value-of select="position()"/>.</fo:block></fo:table-cell></xsl:if>
                        <fo:table-cell xsl:use-attribute-sets="cell"><fo:block wrap-option="wrap" linefeed-treatment="preserve" white-space-collapse="false"><xsl:value-of select="description"/></fo:block></fo:table-cell>
                        <fo:table-cell xsl:use-attribute-sets="cell"><fo:block text-align="right" font-weight="bold" wrap-option="no-wrap"><xsl:value-of select="amount"/></fo:block></fo:table-cell>
                    </fo:table-row>
                </xsl:for-each>
            </fo:table-body>
        </fo:table>
    </xsl:template>

    <xsl:template name="summary">
        <fo:block background-color="#edf3f9" border-left="1mm solid #144270" padding="3mm 4mm" color="#144270" margin-left="0mm" margin-right="0mm"
                  keep-together.within-page="always" keep-with-previous.within-page="always" space-before="3mm">
            <fo:block font-size="10pt" font-weight="bold"><xsl:value-of select="summary/label"/></fo:block>
            <fo:block font-size="18pt" font-weight="bold" text-align="right" space-before="1mm"><xsl:value-of select="summary/amount"/></fo:block>
        </fo:block>
        <fo:block font-size="7.5pt" color="#667484" space-before="2mm" space-after="3mm" keep-with-previous.within-page="always">Słownie: <xsl:value-of select="summary/words"/>.</fo:block>
    </xsl:template>
</xsl:stylesheet>
