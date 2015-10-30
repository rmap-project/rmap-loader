<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
    version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"

    xmlns:kernel3="http://datacite.org/schema/kernel-3"
    xmlns:kernel2-2="http://datacite.org/schema/kernel-2.2"
    
    xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
    xmlns:oai2_record="http://www.openarchives.org/OAI/2.0/"

    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
    xmlns:owl="http://www.w3.org/2002/07/owl#"
    xmlns:cito="http://purl.org/spar/cito/"
    xmlns:cito4data="http://purl.org/spar/cito4data/"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:dcmitype="http://purl.org/dc/dcmitype/"
    xmlns:dcterms="http://purl.org/dc/terms/"
    xmlns:datacite="http://purl.org/spar/datacite/"
    xmlns:fabio="http://purl.org/spar/fabio/"
    xmlns:foaf="http://xmlns.com/foaf/0.1/"
    xmlns:frapo="http://purl.org/cerif/frapo/"
    xmlns:frbr="http://purl.org/vocab/frbr/core#"
    xmlns:ore="http://www.openarchives.org/ore/terms/"
    xmlns:pro="http://purl.org/spar/pro/"
    xmlns:scoro="http://purl.org/spar/scoro/"
    
    xmlns:lookup="http://example.com/lookup"
    xmlns:date="http://exslt.org/dates-and-times"
    xmlns:exsl="http://exslt.org/common"
    extension-element-prefixes="lookup date exsl"
    exclude-result-prefixes="kernel3 lookup">
    
    <xsl:output method="xml" omit-xml-declaration="yes" indent="yes" encoding="UTF-8" />
    
    <xsl:strip-space elements="*"/>
    
    <xsl:param name="agent_record_id" select="'unknown'"/>
    <xsl:param name="agent_name" select="'RMap-Datacite-Harvester'"/>
    <xsl:param name="agent_id" select="'http://rmap-project.org/rmap/agent/RMap-Datacite-Harvester-0.9'"/>
    <xsl:param name="disco_type" select="'http://rmap-project.org/rmap/terms/DiSCO'"/>
    <xsl:param name="temp_disco_id" select="'temp_disco_uri'"/>

    <!-- <xsl:include href="Logging.xsl"/> -->
    <xsl:include href="RMapLookupTable.xsl"/>    

    <!-- current time in W3CDTF profile of ISO 8601: http://www.w3.org/TR/NOTE‐datetime -->
    <!-- <xsl:param name="nowZ"><xsl:value-of select="date:date-time()"/></xsl:param> -->

    <!-- for upper- to lower-case translation -->
    <xsl:variable name="lowercase" select="'abcdefghijklmnopqrstuvwxyzàáâãäåæçèéêëìíîïðñòóôõöøùúûüýþÿžšœ'" />
    <xsl:variable name="uppercase" select="'ABCDEFGHIJKLMNOPQRSTUVWXYZÀÁÂÃÄÅÆÇÈÉÊËÌÍÎÏÐÑÒÓÔÕÖØÙÚÛÜÝÞŸŽŠŒ'" />
    
    <!-- main oai_datacite record handler -->
    <xsl:template match="//oai2_record:record">

        <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
            xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
            xmlns:owl="http://www.w3.org/2002/07/owl#"
            xmlns:rmap="http://rmap-project.org/rmap/terms/"
            xmlns:cito="http://purl.org/spar/cito/"
            xmlns:cito4data="http://purl.org/spar/cito4data/"
            xmlns:dc="http://purl.org/dc/elements/1.1/"
            xmlns:dcmitype="http://purl.org/dc/dcmitype/"
            xmlns:dcterms="http://purl.org/dc/terms/"
            xmlns:datacite="http://purl.org/spar/datacite/"
            xmlns:fabio="http://purl.org/spar/fabio/"
            xmlns:foaf="http://xmlns.com/foaf/0.1/"
            xmlns:frbr="http://purl.org/vocab/frbr/core#"
            xmlns:pro="http://purl.org/spar/pro/"
            xmlns:scoro="http://purl.org/spar/scoro/"
            >
            
            <xsl:variable name="Phase1">
            <xsl:text>&#xa;</xsl:text><xsl:comment> RMap DiSCO </xsl:comment>
            <rmap:DiSCO>
                <!--
                <xsl:attribute name="rdf:about">
                    <xsl:value-of select="$temp_disco_id"/>
                </xsl:attribute>
                -->
      
                <rdf:type>
                    <xsl:attribute name="rdf:resource">
                        <xsl:value-of select="$disco_type"/>
                    </xsl:attribute>
                </rdf:type>
                
                <xsl:if test="$oai_record_datetime">
                    <dcterms:created>
                        <xsl:value-of select="$oai_record_datetime"/>
                    </dcterms:created>
                </xsl:if>
                <dcterms:modified>
                    <xsl:value-of select="$nowZ"/>
                </dcterms:modified>
                
                <!-- Aggregation section -->
                <ore:aggregates>
                    <xsl:attribute name="rdf:resource">
                        <xsl:apply-templates select="//*[local-name() = 'identifier' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]"/>
                    </xsl:attribute>
                </ore:aggregates>
            </rmap:DiSCO>
            
            <!-- Statements describing the first rmap:aggregates resources-->
            <xsl:text>&#xa;</xsl:text>
            <xsl:comment> Aggregated Resources </xsl:comment>
            <rdf:Description>
                <xsl:attribute name="rdf:about">
                    <xsl:apply-templates select="//*[local-name() = 'identifier' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]"/>
                </xsl:attribute>

                <!-- Resource Type(s) -->
                <!-- <rdf:type rdf:resource="http://purl.org/spar/fabio/JournalArticle"/> -->
                <xsl:apply-templates select="//*[local-name() = 'resourceType' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]" />
                
                <!-- alt identifiers -->
                <xsl:apply-templates select=
                    "//*[local-name() = 'alternateIdentifiers' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]" />
                
                <!-- related identifiers -->
                <xsl:apply-templates select="//*[local-name() = 'relatedIdentifiers' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]" />
                
                <!-- titles -->
                <xsl:apply-templates select="//*[local-name() = 'titles' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]" />
                
                <!-- descriptions -->
                <xsl:apply-templates select="//*[local-name() = 'descriptions' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]" />

                <!-- creators and contributors -->
                <xsl:apply-templates select="//*[local-name() = 'creators' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]"/>
                <xsl:apply-templates select="//*[local-name() = 'contributors' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]"/>

                <!-- subject -->
                <xsl:apply-templates select="//*[local-name() = 'subjects' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]" />
            </rdf:Description>
            </xsl:variable>

            <!-- Emit the core DiSCO graph w/ refs to bNodes-->
            <xsl:apply-templates select="exsl:node-set($Phase1)" mode="phase2"/>
            <xsl:text>&#xa;</xsl:text>
            <xsl:comment> Other resources and bNodes </xsl:comment>
            <!-- Emit blank nodes -->
            <xsl:apply-templates select="exsl:node-set($Phase1)" mode="phase3"/>
        </rdf:RDF>
    </xsl:template>

    <!-- Phase 3 emits bNodes -->
    <xsl:template match="//*[child::node()][@rdf:nodeID | @rdf:resource]" mode="phase3">
        <rdf:Description>
            <xsl:choose>
                <xsl:when test="@rdf:nodeID">
                    <xsl:copy-of select="@rdf:nodeID" />
                </xsl:when>
                <xsl:when test="@rdf:resource">
                    <xsl:attribute name="rdf:about">
                        <xsl:value-of select="@rdf:resource"/>
                    </xsl:attribute>
                </xsl:when>
            </xsl:choose>
            <xsl:copy-of select="child::node()"/>
        </rdf:Description>
    </xsl:template>
    <xsl:template match="node()|@*" mode="phase3">
        <!-- don't copy these, just traverse them -->
        <xsl:apply-templates select="node()|@*" mode="phase3"/>
    </xsl:template>

    <!-- Phase 2 emits graph with with only references to bNodes -->
    <xsl:template match="//*[child::node()][@rdf:nodeID | @rdf:resource]" mode="phase2">
        <xsl:copy> 
            <xsl:copy-of select="@*"/>
        </xsl:copy>
    </xsl:template>
    
     <xsl:template match="node()|@*" mode="phase2">
        <xsl:copy>
            <xsl:apply-templates select="node()|@*" mode="phase2"/>
        </xsl:copy>
    </xsl:template>
    
    <!-- *************************************************************** -->

    <xsl:template match="*[local-name() = 'resourceType' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
        <xsl:for-each select=".">
            <xsl:if test="normalize-space(@resourceTypeGeneral)">
                <xsl:variable name="entries_x">
                    <xsl:call-template name="lookup">
                        <xsl:with-param name="LookupTable" select="$table"/>
                        <xsl:with-param name="index" select="'ResourceTypes'"/>
                        <xsl:with-param name="key" select="string(@resourceTypeGeneral)" />
                    </xsl:call-template>
                </xsl:variable>
                <xsl:variable name="entries" select="exsl:node-set($entries_x)"/>
                <xsl:for-each select="$entries/entry">
                    <rdf:type>
                        <xsl:attribute name="rdf:resource"><xsl:value-of select="@uri"/></xsl:attribute>
                    </rdf:type>
                </xsl:for-each>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>
       
    <xsl:template match="*[local-name() = 'relatedIdentifiers' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
        <xsl:for-each select="*[local-name() = 'relatedIdentifier' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
            <xsl:variable name="identifier">
                <xsl:call-template name="normalize_id">
                    <xsl:with-param name="uri" select="./text()" />
                    <xsl:with-param name="uri_type" select="@relatedIdentifierType" />
                </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="entries_x">
                <xsl:call-template name="lookup">
                    <xsl:with-param name="LookupTable" select="$table"/>
                    <xsl:with-param name="index" select="'RelationTypes'"/>
                    <xsl:with-param name="key" select="string(@relationType)" />
                    <xsl:with-param name="allow_default">yes</xsl:with-param>
                </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="entries" select="exsl:node-set($entries_x)"/>
            <xsl:choose>
                <xsl:when test="count($entries/node()[name()='entry']) &gt; 0">
                    <xsl:for-each select="$entries/node()[name()='entry']">
                        <xsl:variable name="qname" select="$entries/entry/@qname"></xsl:variable>
                        <xsl:element name="{$qname}" >
                            <xsl:attribute name="rdf:resource"><xsl:value-of select="$identifier" /></xsl:attribute>
                        </xsl:element>
                    </xsl:for-each>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="log-warning">
                        <xsl:with-param name="message">CRITICAL: 
                            Lookup error id=<xsl:value-of select="//oai2_record:identifier/text()" /> 
                            on index: 'RelationTypes' 
                            with key: <xsl:value-of select="string(@relationType)"/>
                        </xsl:with-param>
                    </xsl:call-template>
                </xsl:otherwise>
            </xsl:choose>
        </xsl:for-each>
    </xsl:template>
    
    
    <xsl:template match="*[local-name() = 'identifier' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
        <xsl:call-template name="normalize_id">
            <xsl:with-param name="uri" select="./text()" />
            <xsl:with-param name="uri_type" select="@identifierType" />
        </xsl:call-template>
     </xsl:template>


    <xsl:template match="*[local-name() = 'alternateIdentifiers' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
        <xsl:for-each select="*[local-name() = 'alternateIdentifier' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
            <xsl:element name="dc:identifier">
                <xsl:call-template name="normalize_id">
                    <xsl:with-param name="uri" select="./text()" />
                    <xsl:with-param name="uri_type" select="@alternateIdentifierType" />
                </xsl:call-template>
            </xsl:element>
        </xsl:for-each>
    </xsl:template>
    
    

    <xsl:template name="normalize_id">
        <xsl:param name="uri_type"/>
        <xsl:param name="uri"/>
        
        <xsl:if test="string-length($uri) &gt; 0">
            <xsl:variable name="uri_prefix">
                <xsl:variable name="id_type" select="translate($uri_type, $uppercase, $lowercase)"/>
                <xsl:choose>
                    <xsl:when test="string-length($id_type) &gt; 0">
                        <xsl:variable name="lookup_result_x">
                            <xsl:call-template name="lookup">
                                <xsl:with-param name="LookupTable" select="$table"/>
                                <xsl:with-param name="index" select="'IdentifierTypes'"/>
                                <xsl:with-param name="key" select="$id_type" />
                            </xsl:call-template>
                        </xsl:variable>
                        <xsl:variable name="lookup_result" select="exsl:node-set($lookup_result_x)" />
                        <xsl:choose>
                            <xsl:when test="$lookup_result//entry/@prefix">
                                <xsl:value-of select="$lookup_result//entry/@prefix"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="concat($id_type, ':')"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </xsl:when>
                    <xsl:otherwise>
                        <xsl:value-of select="''"/>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:variable>
            
            <xsl:value-of select="concat($uri_prefix, $uri)"/>
        </xsl:if>
    </xsl:template>
    

    <xsl:template match="*[local-name() = 'creators' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
        <xsl:for-each select="*[local-name() = 'creator' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
            <!-- -->
            <xsl:call-template name="contributor">
                <xsl:with-param name="qname" select="'dcterms:creator'" />
            </xsl:call-template>
            <!-- -->
        </xsl:for-each>
    </xsl:template>
    
    
    <xsl:template match="*[local-name() = 'contributors' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
        <xsl:for-each select="*[local-name() = 'contributor' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
            <xsl:variable name="entries_x">
                <xsl:call-template name="lookup">
                    <xsl:with-param name="LookupTable" select="$table"/>
                    <xsl:with-param name="index" select="'ContributorTypes'"/>
                    <xsl:with-param name="key" select="string(@contributorType)" />
                </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="entries" select="exsl:node-set($entries_x)"/>
            <xsl:variable name="qname" select="$entries/entry/@qname"></xsl:variable>
            <xsl:call-template name="contributor">
                <xsl:with-param name="qname" select="$qname" />
            </xsl:call-template>
         </xsl:for-each>
    </xsl:template>
    
    <xsl:template name="contributor">
        <xsl:param name="qname" />        
        <xsl:element name="{$qname}" >
            <xsl:if test="normalize-space(.)">
                <xsl:choose>
                    <xsl:when test="./*[local-name() = 'nameIdentifier']/text()">
                        <!-- dcterms:contributor node with the agent identifier -->
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="normalize-space(*[local-name() = 'nameIdentifier' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]/text())" />
                        </xsl:attribute>
                        <!-- make a foaf:name containing the name -->
                        <xsl:if test="normalize-space(*[local-name() = 'creatorName' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]/text())">
                            <xsl:element name="foaf:name">
                                <xsl:value-of select="normalize-space(*[local-name() = 'creatorName' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]/text())" />
                             </xsl:element>
                        </xsl:if>
                    </xsl:when>
                    <xsl:when test="./*[(local-name() = 'creatorName' or local-name() = 'contributorName') and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
                        <!-- add a ref for this RDF blank node -->
                        <xsl:attribute name="rdf:nodeID">
                            <xsl:value-of select="generate-id()" />
                        </xsl:attribute>
                        
                        <!-- make a foaf:name containing the name -->
                        <xsl:element name="foaf:name">
                            <xsl:value-of select="normalize-space(*[local-name() = 'creatorName' or local-name() = 'contributorName'])"></xsl:value-of>
                        </xsl:element>
                    </xsl:when>
                </xsl:choose>
            </xsl:if>
            <rdf:type rdf:resource="http://purl.org/dc/terms/Agent"/>
        </xsl:element>
        
    </xsl:template>


    <xsl:template match="*[local-name() = 'titles' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
        <xsl:for-each select="*[local-name() = 'title' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
            <xsl:element name="dc:title">
                <xsl:if test="@xml:lang">
                    <xsl:attribute name="xml:lang"><xsl:value-of select="@xml:lang"/></xsl:attribute>
                </xsl:if>
                <xsl:value-of select="."/>
            </xsl:element>
        </xsl:for-each>
    </xsl:template>
    
    
    <xsl:template match="*[local-name() = 'descriptions' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
        <xsl:for-each select="*[local-name() = 'description' and starts-with(namespace-uri(), 'http://datacite.org/schema/kernel-' )]">
            <xsl:variable name="entries_x">
                <xsl:call-template name="lookup">
                    <xsl:with-param name="LookupTable" select="$table"/>
                    <xsl:with-param name="index" select="'DescriptionTypes'"/>
                    <xsl:with-param name="key" select="string(@descriptionType)" />
                </xsl:call-template>
            </xsl:variable>
            <xsl:variable name="entries" select="exsl:node-set($entries_x)"/>
            <xsl:variable name="qname" select="$entries/entry/@qname"></xsl:variable>
            <xsl:element name="{$qname}" >
                <xsl:if test="normalize-space(.)">
                    <xsl:if test="@xml:lang">
                        <xsl:attribute name="xml:lang"><xsl:value-of select="@xml:lang" /></xsl:attribute>
                    </xsl:if>
                    <xsl:value-of select="."/>
                </xsl:if>
            </xsl:element>
        </xsl:for-each>
    </xsl:template>    
    

    
     <!-- make sure we don't get text from unhandled nodes and attributes -->
    <xsl:template match="text()|@*">
        <xsl:apply-templates select="node()"/>
    </xsl:template>


    <xsl:template match="*">
        <xsl:call-template name="log-warning">
            <xsl:with-param name="message">
                Unmatched element: <xsl:value-of select="name()"/>
            </xsl:with-param>
        </xsl:call-template>
        <xsl:apply-templates/>
    </xsl:template>
    
    
    
</xsl:stylesheet>