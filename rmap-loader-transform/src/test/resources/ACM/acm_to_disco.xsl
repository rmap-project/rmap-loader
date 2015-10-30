<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns:foaf="http://xmlns.com/foaf/0.1/"
    xmlns:dcterms="http://purl.org/dc/terms/" xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
    xmlns:vcard="http://www.w3.org/2006/vcard/ns#" xmlns:frbr="http://purl.org/vocab/frbr/core#"
    xmlns:pro="http://purl.org/spar/pro/" version="2.0">

    <xsl:output method="xml" omit-xml-declaration="yes" indent="yes" encoding="UTF-8"/>

    <xsl:strip-space elements="*"/>

    <xsl:param name="agent_id" select="'http://rmap-project.org/rmap/agent/RMap-ACM-Harvester-0.1'"/>
    <xsl:param name="disco_type" select="'http://rmap-project.org/rmap/terms/DiSCO'"/>
    <xsl:param name="description"
        select="'Description of ACM Transactions on Mathematical Software article.'"/>
    <xsl:param name="publisher_url"
        select="'http://dbpedia.org/resource/Association_for_Computing_Machinery'"/>
    <xsl:param name="doi_prefix" select="'http://dx.doi.org/'"/>
    <xsl:param name="citation_prefix" select="'http://dl.acm.org/citation.cfm?id='"/>
    <xsl:param name="org_prefix" select="'http://dl.acm.org/inst_page.cfm?id='"/>
    <xsl:param name="software_prefix" select="'http://netlib.org/toms/'"/>

    <xsl:param name="issn" select="concat('urn:issn:',/periodical/journal_rec/issn)"/>
    <xsl:param name="volume" select="/periodical/issue_rec/volume"/>
    <xsl:param name="issue" select="/periodical/issue_rec/issue"/>
    <xsl:param name="publication_date" select="/periodical/issue_rec/publication_date"/>

    <xsl:template match="/periodical/content/article_rec">

        <xsl:result-document method="xml"
            href="acm_toms_v{$volume}i{$issue}_article{position()}_disco.xml">

            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:owl="http://www.w3.org/2002/07/owl#" xmlns:foaf="http://xmlns.com/foaf/0.1/"
                xmlns:rmap="http://rmap-project.org/rmap/terms/"
                xmlns:dcterms="http://purl.org/dc/terms/" xmlns:pro="http://purl.org/spar/pro/"
                xmlns:bibo="http://purl.org/ontology/bibo/"
                xmlns:fabio="http://purl.org/spar/fabio/"
                xmlns:ore="http://www.openarchives.org/ore/terms/"
                xmlns:cito="http://purl.org/spar/cito/"
                xmlns:frbr="http://purl.org/vocab/frbr/core#"
                xmlns:vcard="http://www.w3.org/2006/vcard/ns#">

                <xsl:variable name="doi" select="concat($doi_prefix,doi_number)"/>

                <xsl:text>&#xa;&#xa;</xsl:text>
                <xsl:comment> DISCO DETAILS </xsl:comment>
                <xsl:text>&#xa;</xsl:text>
                <rmap:DiSCO>
                    <rdf:type>
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="$disco_type"/>
                        </xsl:attribute>
                    </rdf:type>
                    <dcterms:creator>
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="$agent_id"/>
                        </xsl:attribute>
                    </dcterms:creator>
                    <dcterms:description>
                        <xsl:value-of select="$description"/>
                    </dcterms:description>
                    <!-- Aggregation section -->
                    <ore:aggregates>
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="$doi"/>
                        </xsl:attribute>
                    </ore:aggregates>

                    <xsl:for-each select="supplements/supple">
                        <ore:aggregates>
                            <xsl:attribute name="rdf:resource">
                                <xsl:value-of select="concat($software_prefix,supple_file)"/>
                            </xsl:attribute>
                        </ore:aggregates>
                    </xsl:for-each>
                </rmap:DiSCO>

                <xsl:comment> ARTICLE DETAILS </xsl:comment>
                <rdf:Description>
                    <xsl:attribute name="rdf:about">
                        <xsl:value-of select="$doi"/>
                    </xsl:attribute>
                    <rdf:type>
                        <xsl:attribute name="rdf:resource"
                            >http://purl.org/spar/fabio/JournalArticle</xsl:attribute>
                    </rdf:type>
                    <rdfs:seeAlso>
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="concat($citation_prefix,article_id)"/>
                        </xsl:attribute>
                    </rdfs:seeAlso>
                    <dcterms:isPartOf>
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="$issn"/>
                        </xsl:attribute>
                    </dcterms:isPartOf>
                    <dcterms:publisher>
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="$publisher_url"/>
                        </xsl:attribute>
                    </dcterms:publisher>
                    <dcterms:rightsHolder>
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="$publisher_url"/>
                        </xsl:attribute>
                    </dcterms:rightsHolder>
                    <dcterms:title>
                        <xsl:value-of select="title"/>
                    </dcterms:title>

                    <xsl:if test="abstract/par and string-length(abstract/par)>0">
                        <dcterms:abstract>
                            <xsl:value-of select="abstract/par"/>
                        </dcterms:abstract>
                    </xsl:if>

                    <xsl:for-each select="authors/au">
                        <xsl:variable name="i" select="position()"/>
                        <dcterms:creator>
                            <xsl:choose>
                                <xsl:when test="orcid_id and string-length(orcid_id)>0">
                                    <xsl:attribute name="rdf:resource">
                                        <xsl:value-of select="orcid_id"/>
                                    </xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:attribute name="rdf:nodeID">
                                        <xsl:value-of select="concat('author',$i)"/>
                                    </xsl:attribute>
                                </xsl:otherwise>
                            </xsl:choose>
                        </dcterms:creator>
                    </xsl:for-each>

                    <bibo:authorList>
                        <rdf:Seq>
                            <xsl:for-each select="authors/au">
                                <xsl:variable name="i" select="position()"/>
                                <rdf:li>
                                    <xsl:choose>
                                        <xsl:when test="orcid_id and string-length(orcid_id)>0">
                                            <xsl:attribute name="rdf:resource">
                                                <xsl:value-of select="orcid_id"/>
                                            </xsl:attribute>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <xsl:attribute name="rdf:nodeID">
                                                <xsl:value-of select="concat('author',$i)"/>
                                            </xsl:attribute>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </rdf:li>
                            </xsl:for-each>
                        </rdf:Seq>
                    </bibo:authorList>

                    <xsl:variable name="month" select="substring(article_publication_date,1,2)"/>
                    <xsl:variable name="day" select="substring(article_publication_date,4,2)"/>
                    <xsl:variable name="year" select="substring(article_publication_date,7,4)"/>
                    <dcterms:issued>
                        <xsl:attribute name="rdf:datatype"
                            >http://www.w3.org/2001/XMLSchema#date</xsl:attribute>
                        <xsl:value-of select="concat($year,'-',$month,'-',$day)"/>
                    </dcterms:issued>

                    <bibo:volume>
                        <xsl:value-of select="$volume"/>
                    </bibo:volume>
                    <bibo:issue>
                        <xsl:value-of select="$issue"/>
                    </bibo:issue>

                    <xsl:comment> CITATIONS LIST </xsl:comment>

                    <xsl:for-each select="references/ref">
                        <xsl:variable name="i" select="position()"/>
                        <cito:cites>
                            <xsl:choose>
                                <xsl:when test="ref_obj_id">
                                    <xsl:attribute name="rdf:resource">
                                        <xsl:value-of select="concat($citation_prefix,ref_obj_id)"
                                        />
                                    </xsl:attribute>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:attribute name="rdf:nodeID">
                                        <xsl:value-of select="concat('citation',$i)"/>
                                    </xsl:attribute>
                                </xsl:otherwise>
                            </xsl:choose>
                        </cito:cites>
                    </xsl:for-each>

                    <xsl:for-each select="cited_by_list/cited_by">
                        <xsl:variable name="i" select="position()"/>
                        <cito:isCitedBy>
                            <xsl:attribute name="rdf:resource">
                                <xsl:value-of select="concat($citation_prefix,cited_by_object_id)"/>
                            </xsl:attribute>
                        </cito:isCitedBy>
                    </xsl:for-each>

                    <xsl:comment> SUPPLEMENTARY MATERIAL LIST </xsl:comment>

                    <xsl:for-each select="supplements/supple">
                        <frbr:supplement>
                            <xsl:attribute name="rdf:resource">
                                <xsl:value-of select="concat($software_prefix,supple_file)"/>
                            </xsl:attribute>
                        </frbr:supplement>
                    </xsl:for-each>
                </rdf:Description>


                <xsl:comment> AUTHOR DETAILS </xsl:comment>
                <xsl:apply-templates select="authors/au"/>


                <xsl:comment> CITATION DETAILS </xsl:comment>
                <xsl:apply-templates select="references/ref"/>


                <xsl:comment> CITED BY DETAILS </xsl:comment>
                <xsl:apply-templates select="cited_by_list/cited_by"/>


                <xsl:comment> SUPPLEMENTARY FILES </xsl:comment>
                <xsl:apply-templates select="supplements/supple">
                    <xsl:with-param name="doi">
                        <xsl:value-of select="$doi"/>
                    </xsl:with-param>
                </xsl:apply-templates>

            </rdf:RDF>
        </xsl:result-document>
    </xsl:template>

    <xsl:template match="authors/au">
        <xsl:variable name="i" select="position()"/>
        <rdf:Description>
            <xsl:choose>
                <xsl:when test="orcid_id and string-length(orcid_id)>0">
                    <xsl:attribute name="rdf:about">
                        <xsl:value-of select="orcid_id"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="rdf:nodeID">
                        <xsl:value-of select="concat('author',$i)"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <dcterms:identifier>
                <xsl:value-of select="person_id"/>
            </dcterms:identifier>

            <xsl:choose>
                <xsl:when test="first_name = 'CORPORATE'">
                    <rdf:type>
                        <xsl:attribute name="rdf:resource"
                            >http://xmlns.com/foaf/0.1/Agent</xsl:attribute>
                    </rdf:type>
                    <foaf:name>
                        <xsl:value-of select="last_name"/>
                    </foaf:name>
                </xsl:when>
                <xsl:otherwise>
                    <rdf:type>
                        <xsl:attribute name="rdf:resource"
                            >http://xmlns.com/foaf/0.1/Person</xsl:attribute>
                    </rdf:type>
                    <foaf:givenName>
                        <xsl:choose>
                            <xsl:when test="middle_name and string-length(middle_name)>0">
                                <xsl:value-of select="concat(first_name,' ', middle_name)"/>
                            </xsl:when>
                            <xsl:otherwise>
                                <xsl:value-of select="first_name"/>
                            </xsl:otherwise>
                        </xsl:choose>
                    </foaf:givenName>
                    <foaf:familyName>
                        <xsl:value-of select="last_name"/>
                    </foaf:familyName>
                </xsl:otherwise>
            </xsl:choose>
            <xsl:if test="string-length(affiliation)>0">
                <pro:holdsRoleInTime>
                    <xsl:attribute name="rdf:nodeID">
                        <xsl:value-of select="concat('org_role',$i)"/>
                    </xsl:attribute>
                </pro:holdsRoleInTime>
            </xsl:if>
        </rdf:Description>

        <xsl:comment> AUTHOR ROLES / AFFILIATION </xsl:comment>
        <xsl:if test="string-length(affiliation)>0">
            <rdf:Description>
                <xsl:attribute name="rdf:nodeID">
                    <xsl:value-of select="concat('org_role',$i)"/>
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
                        <xsl:value-of select="concat('org',$i)"/>
                    </xsl:attribute>
                </pro:relatesToOrganization>
            </rdf:Description>

            <rdf:Description>
                <xsl:attribute name="rdf:nodeID">
                    <xsl:value-of select="concat('org',$i)"/>
                </xsl:attribute>
                <rdf:type>
                    <xsl:attribute name="rdf:resource"
                        >http://xmlns.com/foaf/0.1/Organization</xsl:attribute>
                </rdf:type>
                <xsl:if test="affil_inst_id">
                    <rdfs:seeAlso>
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="concat($org_prefix,affil_inst_id)"/>
                        </xsl:attribute>
                    </rdfs:seeAlso>
                </xsl:if>
                <vcard:extended-address>
                    <xsl:value-of select="affiliation"/>
                </vcard:extended-address>
            </rdf:Description>
        </xsl:if>

    </xsl:template>

    <xsl:template match="references/ref">
        <xsl:variable name="i" select="position()"/>
        <rdf:Description>
            <xsl:choose>
                <xsl:when test="ref_obj_id">
                    <xsl:attribute name="rdf:about">
                        <xsl:value-of select="concat($citation_prefix,ref_obj_id)"/>
                    </xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="rdf:nodeID">
                        <xsl:value-of select="concat('citation',$i)"/>
                    </xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
            <dcterms:bibliographicCitation>
                <xsl:value-of select="ref_text"/>
            </dcterms:bibliographicCitation>
        </rdf:Description>
    </xsl:template>

    <xsl:template match="cited_by_list/cited_by">
        <rdf:Description>
            <xsl:attribute name="rdf:about">
                <xsl:value-of select="concat($citation_prefix,cited_by_object_id)"/>
            </xsl:attribute>
            <dcterms:bibliographicCitation>
                <xsl:value-of select="cited_by_text"/>
            </dcterms:bibliographicCitation>
        </rdf:Description>
    </xsl:template>

    <xsl:template match="supplements/supple">
        <xsl:param name="doi"/>
        <rdf:Description>
            <xsl:attribute name="rdf:about">
                <xsl:value-of select="concat($software_prefix,supple_file)"/>
            </xsl:attribute>
            <rdf:type>
                <xsl:attribute name="rdf:resource"
                    >http://purl.org/spar/fabio/Algorithm</xsl:attribute>
            </rdf:type>
            <dcterms:title>
                <xsl:value-of select="supple_text"/>
            </dcterms:title>
            <frbr:supplementOf>
                <xsl:attribute name="rdf:resource">
                    <xsl:value-of select="$doi"/>
                </xsl:attribute>
            </frbr:supplementOf>
        </rdf:Description>
    </xsl:template>

</xsl:stylesheet>
