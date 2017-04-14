<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
    xmlns:lookup="http://example.com/lookup"
    xmlns:exsl="http://exslt.org/common"
    extension-element-prefixes="lookup exsl"
    exclude-result-prefixes="lookup"
    version="1.0">

    <!-- ***************************************************************
         Lookup table indexes
         *************************************************************** -->
    <xsl:variable name='table' select='document("")//lookup:table' />
    <xsl:key name='ResourceTypes'   match='ResourceTypes/entry'   use='@id' />   
    <xsl:key name='RelationTypes'   match='RelationTypes/entry'   use='@id' />   
    <xsl:key name='IdentifierTypes' match='IdentifierTypes/entry' use='@id' />
    <xsl:key name='DescriptionTypes' match='DescriptionTypes/entry' use='@id' />
    <xsl:key name='ContributorTypes' match='ContributorTypes/entry' use='@id' />

    <!-- included files -->
    <xsl:include href="Logging.xsl"/>
    
    <!-- ***************************************************************
         Lookup table
         Section additions to / deletions from this table may require
         updates to the lookup indexes (see xsl:key elements).
         *************************************************************** -->
    <lookup:table>
        <IdentifierTypes>
            <entry id='doi' prefix='http://dx.doi.org/' />
            <entry id='handle' prefix='http://hdl.handle.net' />
            <entry id='url' prefix='' />
        </IdentifierTypes>
        <DescriptionTypes>
            <entry id='Abstract' qname='dcterms:abstract' uri='http://purl.org/dc/terms/abstract'/>
            <entry id='Methods' qname='fabio:StandardOperatingProcedure' uri='http://purl.org/spar/fabio/StandardOperatingProcedure'/> 
            <entry id='SeriesInformation' qname='datacite:series-information' uri='http://purl.org/spar/datacite/series-information'/> 
            <entry id='TableOfContents' qname='dcterms:tableOfContents' uri='http://purl.org/dc/terms/tableOfContents'/> 
            <entry id='Other' qname='dcterms:description' uri='http://purl.org/dc/terms/description'/>
        </DescriptionTypes>
        <ContributorTypes>
            <entry id='ContactPerson' qname='scoro:contact-person' uri='http://purl.org/spar/scoro/contact-person' /> 
            <entry id='DataCollector' qname='scoro:data-creator' uri='http://purl.org/spar/scoro/data-creator' /> 
            <entry id='DataManager' qname='scoro:data-manager' uri='http://purl.org/spar/scoro/data-manager' /> 
            <entry id='Distributor' qname='pro:distributor' uri='http://purl.org/spar/pro/distributor' /> 
            <entry id='Editor' qname='pro:editor' uri='http://purl.org/spar/pro/editor' />
            <entry id='Funder' qname='scoro:funder' uri='http://purl.org/spar/scoro/funder' />
            <entry id='HostingInstitution' qname='frapo:HostInstitution' uri='http://purl.org/cerif/frapo/HostInstitution' />
            <entry id='Producer' qname='pro:producer' uri='http://purl.org/spar/pro/producer' />
            <entry id='ProjectLeader' qname='scoro:project-leader' uri='http://purl.org/spar/scoro/project-leader' />
            <entry id='ProjectManager' qname='scoro:project-manager' uri='http://purl.org/spar/scoro/project-manager' />
            <entry id='ProjectMember' qname='scoro:project-member' uri='http://purl.org/spar/scoro/project-member' />
            <entry id='RegistrationAgency' qname='frapo:RegistrationAgency' uri='http://purl.org/cerif/frapo/RegistrationAgency' />
            <entry id='RegistrationAuthority' qname='frapo:RegistrationAuthority' uri='http://purl.org/cerif/frapo/RegistrationAuthority' />
            <entry id='RelatedPerson' qname='scoro:affiliate' uri='http://purl.org/spar/scoro/affiliate' />
            <entry id='Researcher' qname='scoro:researcher' uri='http://purl.org/spar/scoro/researcher' />
            <entry id='ResearchGroup' qname='frapo:ResearchGroup' uri='http://purl.org/cerif/frapo/ResearchGroup' />
            <entry id='RightsHolder' qname='scoro:rights-holder' uri='http://purl.org/spar/scoro/rights-holder' />
            <entry id='Sponsor' qname='scoro:sponsor' uri='http://purl.org/spar/scoro/sponsor' />
            <entry id='Supervisor' qname='scoro:supervisor' uri='http://purl.org/spar/scoro/supervisor' />
            <entry id='WorkPackageLeader' qname='scoro:workpackage-leader' uri='http://purl.org/spar/scoro/workpackage-leader' />
            <entry id='Other' qname='dcterms:contributor' uri='http://purl.org/dc/terms/contributor' />
        </ContributorTypes>
        <ResourceTypes>
            <entry id='Audiovisual' qname='dcmitype:MovingImage' uri='http://purl.org/dc/dcmitype/MovingImage'/>
            <entry id='Collection' qname='dcmitype:Collection' uri='http://purl.org/dc/dcmitype/Collection'/>
            <entry id='Dataset' qname='dcmitype:Dataset' uri='http://purl.org/dc/dcmitype/Dataset'/>
            <entry id='Event' qname='dcmitype:Event' uri='http://purl.org/dc/dcmitype/Event'/>
            <entry id='Image' qname='dcmitype:Image' uri='http://purl.org/dc/dcmitype/Image'/>
            <entry id='InteractiveResource' qname='dcmitype:InteractiveResource' uri='http://purl.org/dc/dcmitype/InteractiveResource'/>
            <entry id='Model' qname='fabio:Model' uri='http://purl.org/spar/fabio/Model'/>
            <entry id='PhysicalObject' qname='dcmitype:PhysicalObject' uri='http://purl.org/dc/dcmitype/PhysicalObject'/>
            <entry id='Service' qname='dcmitype:Service' uri='http://purl.org/dc/dcmitype/Service'/>
            <entry id='Software' qname='dcmitype:Software' uri='http://purl.org/dc/dcmitype/Software'/>            
            <entry id='Sound' qname='dcmitype:Sound' uri='http://purl.org/dc/dcmitype/Sound'/>
            <entry id='Text' qname='dcmitype:Text' uri='http://purl.org/dc/dcmitype/Text'/>
            <entry id='Workflow' qname='fabio:Workflow' uri='http://purl.org/spar/fabio/Workflow'/>
            <entry id='Other' qname='rdfs:Resource' uri='http://www.w3.org/2000/01/rdf-schema#Resource'/>
        </ResourceTypes>
        <RelationTypes>
            <entry id='Compiles' qname='cito:compiles' uri='http://purl.org/spar/cito/compiles'/>
            <entry id='IsCompiledBy' qname='cito:isCompiledBy' uri='http://purl.org/spar/cito/isCompiledBy'/>            
            <entry id='IsCitedBy' qname='cito:isCitedBy' uri='http://purl.org/spar/cito/isCitedBy'/>
            <entry id='Cites' qname='cito:cites' uri='http://purl.org/spar/cito/cites'/>
            <entry id='IsSupplementTo' qname='frbr:supplementOf' uri='http://purl.org/vocab/frbr/core#supplementOf'/>
            <entry id='IsSupplementedBy' qname='frbr:supplement' uri='http://purl.org/vocab/frbr/core#supplement'/>
            <entry id='IsContinuedBy' qname='frbr:successor' uri='http://purl.org/vocab/frbr/core#successor'/>
            <entry id='Continues' qname='frbr:successorOf' uri='http://purl.org/vocab/frbr/core#successorOf'/>
            <entry id='HasMetadata' qname='cito:citesAsMetadataDocument' uri='http://purl.org/spar/cito/citesAsMetadataDocument'/>
            <entry id='IsMetadataFor' qname='cito:isCitedAsMetadataDocumentBy' uri='http://purl.org/spar/cito/isCitedAsMetadataDocumentBy'/>
            <entry id='IsNewVersionOf' qname='dcterms:isVersionOf' uri='http://purl.org/dc/terms/isVersionOf'/>
            <entry id='IsPreviousVersionOf' qname='dcterms:hasVersion' uri='http://purl.org/dc/terms/hasVersion'/>
            <entry id='IsPartOf' qname='dcterms:isPartOf' uri='http://purl.org/dc/terms/isPartOf'/>
            <entry id='HasPart' qname='dcterms:hasPart' uri='http://purl.org/dc/terms/hasPart'/>
            <entry id='IsReferencedBy' qname='cito:isCitedForInformationBy' uri='http://purl.org/spar/cito/isCitedForInformationBy'/>
            <entry id='References' qname='cito:citesForInformation' uri='http://purl.org/spar/cito/citesForInformation'/>
            <entry id='IsDocumentedBy' qname='cito:isDocumentedBy' uri='http://purl.org/spar/cito/isDocumentedBy'/>
            <entry id='Documents' qname='cito:documents' uri='http://purl.org/spar/cito/documents'/>
            <entry id='IsCompiledBy' qname='cito:isCompiledBy' uri='http://purl.org/spar/cito4data/isCompiledBy'/>
            <entry id='Compiles' qname='cito:compiles' uri='http://purl.org/spar/cito4data/compiles'/>
            <entry id='IsVariantFormOf' qname='frbr:alternateOf' uri='http://purl.org/vocab/frbr/core#alternateOf'/>
            <entry id='IsOriginalFormOf' qname='frbr:alternate' uri='http://purl.org/vocab/frbr/core#alternate'/>
            <entry id='IsIdenticalTo' qname='owl:sameAs' uri='http://www.w3.org/2002/07/owl#sameAs'/>
            
            <entry id='IsDerivedFrom' qname='dcterms:source' uri='http://purl.org/dc/terms/source'/>
            <entry id='default' qname='dcterms:related' uri='http://purl.org/dc/terms/related'/>
        </RelationTypes>
    </lookup:table>

    <!-- Lookup Table function -->
    <xsl:template name="lookup">
        <xsl:param name="index"/>
        <xsl:param name="key"/>
        <xsl:param name="LookupTable"/>
        <xsl:param name="allow_default">no</xsl:param>
        <xsl:param name="fatal">no</xsl:param>
        <!-- check the fixed value arguments. Fatal error, if invalid. -->
        <xsl:if test="$allow_default != 'no' and $allow_default != 'yes'">
            <xsl:call-template name="log-fatal">
                <xsl:with-param name="message">
                    template "lookup" param "allow_default" must be either "yes" or "no"
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:if test="$fatal != 'no' and $fatal != 'yes'">
            <xsl:call-template name="log-fatal">
                <xsl:with-param name="message">
                    template "lookup" param "fatal" must be either "yes" or "no"
                </xsl:with-param>
            </xsl:call-template>
        </xsl:if>
        <xsl:variable name="key_text" select="string($key)"/>
        
        <!-- Perform the lookup -->
        <xsl:variable name="result">
            <xsl:for-each select='$LookupTable'>
                <xsl:variable name="res" select="key($index, $key_text)"/>
                <xsl:copy-of select="$res" />
            </xsl:for-each>
        </xsl:variable>
        <xsl:choose>
            <!-- if lookup was successful, then send the result back -->
            <xsl:when test="count(exsl:node-set($result)/node()) &gt; 0">
                <xsl:copy-of select="$result"/>
            </xsl:when>
            <!-- if we can't use a default, then fail here -->
            <xsl:when test="$allow_default = 'no'">
                <xsl:call-template name="log-error">
                    <xsl:with-param name="message">
                        Lookup error
                        on index: <xsl:value-of select="$index"/>
                        with key: <xsl:value-of select="$key_text"/>
                        -- no default permitted
                    </xsl:with-param>
                </xsl:call-template>                
            </xsl:when>
            
            <!-- for unsuccessful lookup, check for default entry(ies) -->
            <xsl:otherwise>
                <xsl:variable name="default">
                    <!-- <xsl:element name="entries"> -->
                    <xsl:for-each select='$LookupTable'>
                        <xsl:variable name="res" select="key($index, 'default')"/>
                        <xsl:copy-of select="$res" />
                    </xsl:for-each>
                </xsl:variable>
                <xsl:choose>
                    <!-- if we got a default, send that back as the result -->
                    <xsl:when test="count(exsl:node-set($default)/node()) &gt; 0">
                        <xsl:copy-of select="$default"/>
                        <!-- emit a warning that we had to use the default -->
                        <xsl:call-template name="log-warning">
                            <xsl:with-param name="message">
                                Lookup error 
                                on index: <xsl:value-of select="$index"/>
                                with key: <xsl:value-of select="$key_text"/>
                                -- using default
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:when>
                    <!-- if failed to get default, then emit error -->
                    <xsl:otherwise>
                        <xsl:call-template name="log-error">
                            <xsl:with-param name="message">
                                Lookup error 
                                on index: <xsl:value-of select="$index"/>
                                with key: <xsl:value-of select="$key_text"/>
                                -- no default
                            </xsl:with-param>
                        </xsl:call-template>
                    </xsl:otherwise>
                </xsl:choose>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:template>
    
    <!-- Test lookup table function
    <xsl:template match="/">
        <xsl:variable name="myresult">
            <xsl:call-template name="lookup">
                <xsl:with-param name="LookupTable" select="$table"/>
                <xsl:with-param name="index" select="'ResourceTypes'"/>
                <xsl:with-param name="key" select="'Software'" />
            </xsl:call-template>
        </xsl:variable>
        <xsl:copy-of select="$myresult"/>
        <xsl:variable name="myresult2">
            <xsl:call-template name="lookup">
                <xsl:with-param name="LookupTable" select="$table"/>
                <xsl:with-param name="index" select="'RelationTypes'"/>
                <xsl:with-param name="key" select="'Compiles'" />
            </xsl:call-template>
        </xsl:variable>
        <xsl:copy-of select="$myresult2"/>
    </xsl:template>
    -->

</xsl:stylesheet>