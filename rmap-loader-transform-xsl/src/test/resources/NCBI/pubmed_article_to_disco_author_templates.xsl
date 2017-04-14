<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
        xmlns:xs="http://www.w3.org/2001/XMLSchema"
        xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
        xmlns:dcterms="http://purl.org/dc/terms/" 
        xmlns:foaf="http://xmlns.com/foaf/"
        xmlns:pro="http://purl.org/spar/pro/" 
        xmlns:bibo="http://purl.org/ontology/bibo/"
        xmlns:vcard="http://www.w3.org/2006/vcard/ns#" 
        exclude-result-prefixes="xs"
        version="2.0">
    
    
    <xsl:template match="AuthorList">
        <xsl:for-each select="Author">
            <xsl:variable name="i" select="position()"/>
            <dcterms:creator>
                <xsl:apply-templates select="." mode="choose_author_id">
                    <xsl:with-param name="i" select="$i"/>
                    <xsl:with-param name="isDescribedItem" select="0"/>
                </xsl:apply-templates> 
            </dcterms:creator>
        </xsl:for-each>
        
        <bibo:authorList>
            <rdf:Seq>
                <xsl:for-each select="Author">
                    <xsl:variable name="i" select="position()"/>
                    <rdf:li>
                        <xsl:apply-templates select="." mode="choose_author_id">
                            <xsl:with-param name="i" select="$i"/>
                            <xsl:with-param name="isDescribedItem" select="0"/>
                        </xsl:apply-templates> 
                    </rdf:li>
                </xsl:for-each>
            </rdf:Seq>
        </bibo:authorList>
    </xsl:template>
    
    <!-- Determines whether to use blanknode or ORCID for Author ID-->
    <xsl:template match="AuthorList/Author" mode="choose_author_id">
        <xsl:param name="i"/>
        <xsl:param name="isDescribedItem"/>
        <xsl:choose>
            <xsl:when test="string-length(Identifier[@Source='ORCID'])>0 and $isDescribedItem eq 1">
                <xsl:attribute name="rdf:about" select="Identifier[@Source='ORCID']"/>
            </xsl:when>
            <xsl:when test="string-length(Identifier[@Source='ORCID'])>0 and $isDescribedItem eq 0">
                <xsl:attribute name="rdf:resource" select="Identifier[@Source='ORCID']"/>
            </xsl:when>
            <xsl:otherwise>        
                <xsl:attribute name="rdf:nodeID" select="concat('author',$i)"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- Generates Author Description -->   
    <xsl:template match="AuthorList/Author" mode="author_details">
        <xsl:variable name="i" select="position()"/>
        <rdf:Description>           
            <xsl:apply-templates select="." mode="choose_author_id">
                <xsl:with-param name="i" select="$i"/>
                <xsl:with-param name="isDescribedItem" select="1"/>
            </xsl:apply-templates>               
            
            <rdf:type>
                <xsl:attribute name="rdf:resource"
                    >http://xmlns.com/foaf/0.1/Person</xsl:attribute>
            </rdf:type>
            <foaf:givenName>
                <xsl:value-of select="ForeName"/>
            </foaf:givenName>
            <foaf:familyName>
                <xsl:value-of select="LastName"/>
            </foaf:familyName>
            
            <xsl:apply-templates select="AffiliationInfo/Affiliation" mode="affillist">
                <xsl:with-param name="i" select="$i"/>
            </xsl:apply-templates> 
            
        </rdf:Description>
        
        <xsl:apply-templates select="AffiliationInfo/Affiliation" mode="affildetails">
            <xsl:with-param name="i" select="$i"/>
        </xsl:apply-templates> 
        
    </xsl:template>
    
    <xsl:template match="AffiliationInfo/Affiliation" mode="affillist">   
        <xsl:param name="i"/>
        <xsl:variable name="affilnum" select="position()"/>
        <xsl:if test="string-length(.)>0">
            <pro:holdsRoleInTime>
                <xsl:attribute name="rdf:nodeID">
                    <xsl:value-of select="concat('org_role',$i,'_',$affilnum)"/>
                </xsl:attribute>
            </pro:holdsRoleInTime>
        </xsl:if>
    </xsl:template>
    
    <xsl:template match="AffiliationInfo/Affiliation" mode="affildetails">  
        <xsl:param name="i"/>
        <xsl:variable name="affilnum" select="position()"/>        
        <xsl:if test="string-length(.)>0">
            <rdf:Description>
                <xsl:attribute name="rdf:nodeID">
                    <xsl:value-of select="concat('org_role',$i,'_',$affilnum)"/>
                </xsl:attribute>
                <rdf:type>
                    <xsl:attribute name="rdf:resource"
                        >http://purl.org/spar/pro/RoleInTime</xsl:attribute>
                </rdf:type>
                <pro:withRole>
                    <xsl:attribute name="rdf:resource"
                        >http://purl.org/spar/scoro/affiliate</xsl:attribute>
                </pro:withRole>
                <pro:relatesToOrganization>
                    <xsl:attribute name="rdf:nodeID">
                        <xsl:value-of select="concat('org',$i,'_',$affilnum)"/>
                    </xsl:attribute>
                </pro:relatesToOrganization>
            </rdf:Description>
            
            <rdf:Description>
                <xsl:attribute name="rdf:nodeID">
                    <xsl:value-of select="concat('org',$i,'_',$affilnum)"/>
                </xsl:attribute>
                <rdf:type>
                    <xsl:attribute name="rdf:resource"
                        >http://xmlns.com/foaf/0.1/Organization</xsl:attribute>
                </rdf:type>
                <vcard:extended-address>
                    <xsl:value-of select="."/>
                </vcard:extended-address>
            </rdf:Description>
        </xsl:if>
        
    </xsl:template>
    
    
</xsl:stylesheet>