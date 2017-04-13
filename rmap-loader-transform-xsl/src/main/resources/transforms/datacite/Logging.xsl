<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:exsl="http://exslt.org/common"
    xmlns:date="http://exslt.org/dates-and-times"
    extension-element-prefixes="date exsl"
    exclude-result-prefixes="date xs"
    version="1.0">
    
    <!-- current time in W3CDTF profile of ISO 8601: http://www.w3.org/TR/NOTE‐datetime -->
    <xsl:param name="nowZ"><xsl:value-of select="date:date-time()"/></xsl:param>

    <xsl:param name="log_id" select="'(unknown)'"/>
    <xsl:param name="log_message_prefix" select="'RMap'"/>
    <xsl:param name="log_fatal_prefix"   select="'FATAL'"/>
    <xsl:param name="log_error_prefix"   select="'ERROR'"/>
    <xsl:param name="log_warning_prefix" select="'WARNING'"/>
    <xsl:param name="log_info_prefix"    select="'INFO'"/>
    <xsl:param name="log_debug_prefix"   select="'DEBUG'"/>
    
    <xsl:template name="log-debug">
        <xsl:param name="message"/>
        <xsl:call-template name="log">
            <xsl:with-param name="message">
                <xsl:value-of select="$log_debug_prefix"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="$message"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="log-info">
        <xsl:param name="message"/>
        <xsl:call-template name="log">
            <xsl:with-param name="message">
                <xsl:value-of select="$log_info_prefix"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="$message"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="log-warning">
        <xsl:param name="message"/>
        <xsl:call-template name="log">
            <xsl:with-param name="message">
                <xsl:value-of select="$log_warning_prefix"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="$message"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="log-error">
        <xsl:param name="message"/>
        <xsl:call-template name="log">
            <xsl:with-param name="message">
                <xsl:value-of select="$log_error_prefix"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="$message"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="log-fatal">
        <xsl:param name="message"/>
        <xsl:call-template name="log">
            <xsl:with-param name="terminate">yes</xsl:with-param>
            <xsl:with-param name="message">
                <xsl:value-of select="$log_fatal_prefix"/>
                <xsl:text> </xsl:text>
                <xsl:value-of select="$message"/>
            </xsl:with-param>
        </xsl:call-template>
    </xsl:template>
    
    <xsl:template name="log">
        <xsl:param name="message"/>
        <xsl:param name="terminate">no</xsl:param>
        <!-- check the fixed value arguments. Fatal error, if invalid. -->
        <xsl:if test="$terminate != 'no' and $terminate != 'yes'">
            <xsl:call-template name="log-fatal">
                <xsl:with-param name="message">
                    template "lookup" param "terminate" must be either "yes" or "no"
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:variable name="log_message">
            <xsl:value-of select="$nowZ"/>
            [<xsl:value-of select="$log_id"/>]
            <xsl:text> </xsl:text>
            <xsl:value-of select="$log_message_prefix"/>
            <xsl:text> </xsl:text>
            <xsl:value-of select="$message"/>
        </xsl:variable>
        <xsl:choose>
            <xsl:when test="$terminate='yes'">
                <xsl:message terminate="yes">
                    <xsl:value-of select="normalize-space($log_message)"/>
                </xsl:message>
            </xsl:when>
            <xsl:otherwise>
                <xsl:message terminate="no">
                    <xsl:value-of select="normalize-space($log_message)"/>
                </xsl:message>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

</xsl:stylesheet>