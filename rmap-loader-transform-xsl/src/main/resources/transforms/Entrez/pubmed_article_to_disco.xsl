<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
    xmlns:dcterms="http://purl.org/dc/terms/" 
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:frbr="http://purl.org/vocab/frbr/core#"
    xmlns:ore="http://www.openarchives.org/ore/terms/" 
    xmlns:bibo="http://purl.org/ontology/bibo/"
    xmlns:cito="http://purl.org/spar/cito/" 
    xmlns:fn="http://example.com/fn/" version="2.0">

    <xsl:output method="xml" omit-xml-declaration="yes" indent="yes" encoding="UTF-8"/>

    <xsl:strip-space elements="*"/>
    
    <!-- disco metadata parameters -->
    <xsl:param name="agent_id" select="'http://rmap-hub.org/agent/RMap-NCBI-Harvester-0.1'"/>
    <xsl:param name="disco_type" select="'http://purl.org/ontology/rmap#DiSCO'"/>
    <xsl:param name="description" select="'Connected identifiers for a PubMed article and any databank relationships.'"/>
    
    <xsl:include href="pubmed_article_to_disco_author_templates.xsl"/>
    <xsl:include href="pubmed_article_to_disco_databank_templates.xsl"/>
    
    <!-- need to configure a lot of url prefixes both for secondary API paths and to form dois from accession numbers
         let's keep these in a separate file -->
    <xsl:param name="issn_prefix" select="'urn:issn:'"/>
    <xsl:param name="doi_prefix" select="'http://dx.doi.org/'"/>
    
    <!-- internal pubmed links -->
    <xsl:param name="pubmed_prefix" select="'http://www.ncbi.nlm.nih.gov/pubmed/'"/>
    <xsl:param name="pmc_prefix" select="'http://www.ncbi.nlm.nih.gov/pmc/articles/'"/>
        
    <!-- links to access additional article metadata for the xslt -->
    <xsl:param name="article_path" select="'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&amp;retmode=xml&amp;id='"/>
      
        
    <xsl:template match="/eSearchResult/IdList/Id">
        <xsl:variable name="pmid" select="."/>
        <xsl:variable name="article_doc" select="document(concat($article_path,$pmid))"/>                
        <xsl:apply-templates select="$article_doc/PubmedArticleSet/PubmedArticle">
            <xsl:with-param name="pmid" select="$pmid"/>
        </xsl:apply-templates>    
    </xsl:template>
    
    <xsl:template match="PubmedArticleSet/PubmedArticle">
        <xsl:param name="pmid" />
        
        <xsl:variable name="mainArticleLink" 
            select="fn:getMainArticleLink(PubmedData/ArticleIdList/ArticleId[@IdType='doi'], PubmedData/ArticleIdList/ArticleId[@IdType='pubmed'])"/>
        
        <xsl:result-document method="xml"
            href="pubmed_article_{$pmid}_disco.xml">
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
                xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
                xmlns:owl="http://www.w3.org/2002/07/owl#" 
                xmlns:foaf="http://xmlns.com/foaf/"
                xmlns:rmap="http://purl.org/ontology/rmap#"
                xmlns:dcterms="http://purl.org/dc/terms/" 
                xmlns:pro="http://purl.org/spar/pro/"
                xmlns:bibo="http://purl.org/ontology/bibo/"
                xmlns:fabio="http://purl.org/spar/fabio/"
                xmlns:ore="http://www.openarchives.org/ore/terms/"
                xmlns:cito="http://purl.org/spar/cito/"
                xmlns:frbr="http://purl.org/vocab/frbr/core#"
                xmlns:vcard="http://www.w3.org/2006/vcard/ns#"
                xmlns:prov="http://www.w3.org/ns/prov#"
                xmlns:dc="http://purl.org/dc/elements/1.1/"
                xmlns:mets="http://www.loc.gov/METS/"
                xmlns:dim="http://www.dspace.org/xmlns/dspace/dim"  >     
                
                <xsl:text>&#xa;&#xa;</xsl:text>
                <xsl:comment> DISCO DETAILS </xsl:comment>
                <xsl:text>&#xa;</xsl:text>
                <rmap:DiSCO>
                    <dcterms:creator>
                        <xsl:attribute name="rdf:resource">
                            <xsl:value-of select="$agent_id"/>
                        </xsl:attribute>
                    </dcterms:creator>
                    <dcterms:description>
                        <xsl:value-of select="$description"/>
                    </dcterms:description>
                    
                    <!-- Article that is part of the aggregation section -->
                    <ore:aggregates>
                        <xsl:attribute name="rdf:resource" select="$mainArticleLink"/>
                    </ore:aggregates>
                    
                    <!-- Databank links that are part of the aggregation -->
                    <xsl:apply-templates select="MedlineCitation/Article/DataBankList/DataBank" mode="databank_links"/>
                    
                </rmap:DiSCO>
                
                <xsl:comment> ARTICLE DETAILS </xsl:comment>
                <rdf:Description>
                    <!-- grab the primary identifier that we will use - DOI preferred, PMID is secondary -->
                    <xsl:attribute name="rdf:about" select="$mainArticleLink"/>
                    
                    <!-- list other seeAlso identifiers for article -->
                    <xsl:apply-templates select="PubmedData/ArticleIdList" mode="other_article_ids"/>   
                    
                    <!-- get some article metadata -->
                    <xsl:apply-templates select="MedlineCitation/Article"/>
                    
                    <!-- list citation links if available -->
                    <xsl:apply-templates select="MedlineCitation/CommentsCorrectionsList/CommentsCorrections[@RefType='Cites']/PMID[@Version='1']" mode="citeslist"/>
                    
                    <!-- list data bank citations for article -->
                    <xsl:apply-templates select="MedlineCitation/Article/DataBankList/DataBank" mode="dbcites"/>
                    
                </rdf:Description>
                
                <xsl:comment> DATASET DETAILS </xsl:comment>
                <!-- add more details about the connected dataset - what is output varies by databank -->
                <xsl:apply-templates select="MedlineCitation/Article/DataBankList/DataBank" mode="dbdetails">
                    <xsl:with-param name="main_article_id" select="$mainArticleLink"/>
                </xsl:apply-templates>
                
                <xsl:comment> AUTHOR DETAILS </xsl:comment>
                <!-- Author information -->
                <xsl:apply-templates select="MedlineCitation/Article/AuthorList/Author" mode="author_details"/>
                                
                <!-- add more details about cited articles -->
                <xsl:apply-templates select="MedlineCitation/CommentsCorrectionsList/CommentsCorrections[@RefType='Cites']/PMID[@Version='1']" mode="seealsorelations"/>

                
            </rdf:RDF>
        </xsl:result-document>
    </xsl:template>
        
    <xsl:function name="fn:getMainArticleLink">
        <xsl:param name="doi"/>
        <xsl:param name="pmid"/>
        <xsl:choose>
            <xsl:when test="string-length($doi)>0">
                <xsl:value-of select="concat($doi_prefix,$doi)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="concat($pubmed_prefix,$pmid)"/>                
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
        
    <xsl:template match="PubmedData/ArticleIdList" mode="main_article_link">
        
    </xsl:template>
    
    <xsl:template match="PubmedData/ArticleIdList" mode="other_article_ids">  
        <dcterms:identifier>
            <xsl:attribute name="rdf:resource" select="concat('PMID:',ArticleId[@IdType='pubmed'])"/>
        </dcterms:identifier>
        <xsl:if test="string-length(ArticleId[@IdType='doi'])>0">
            <rdfs:seeAlso>
                <xsl:attribute name="rdf:resource">
                    <xsl:value-of select="concat($pubmed_prefix,ArticleId[@IdType='pubmed'])"/>   
                </xsl:attribute>
            </rdfs:seeAlso>
            <dcterms:identifier>
                <xsl:attribute name="rdf:resource" select="concat('doi:',ArticleId[@IdType='doi'])"/>
            </dcterms:identifier>
        </xsl:if>
        
        <xsl:if test="string-length(ArticleId[@IdType='pmc'])>0">
            <dcterms:identifier>
                <xsl:attribute name="rdf:resource" select="concat('PMCID:',ArticleId[@IdType='pmc'])"/>
            </dcterms:identifier>
            <rdfs:seeAlso>
                <xsl:attribute name="rdf:resource">
                    <xsl:value-of select="concat($pmc_prefix,ArticleId[@IdType='pmc'],'/')"/>   
                </xsl:attribute>
            </rdfs:seeAlso>
        </xsl:if>
    </xsl:template>
        
    <xsl:template match="MedlineCitation/CommentsCorrectionsList/CommentsCorrections[@RefType='Cites']/PMID[@Version='1']" mode="citeslist">
            <cito:cites>
                <xsl:attribute name="rdf:resource" select="concat($pubmed_prefix,.)"/>
            </cito:cites>
            <xsl:variable name="citation_doc" select="document(concat($article_path,.))"/>  
            <xsl:variable name="citation_doi" select="$citation_doc/PubmedArticleSet/PubmedArticle/PubmedData/ArticleIdList/ArticleId[@IdType='doi']"/>
            <xsl:if test="string-length($citation_doi)>0">
                <cito:cites>
                    <xsl:attribute name="rdf:resource" select="concat($doi_prefix,$citation_doi)"/>
                </cito:cites>
            </xsl:if>
    </xsl:template>


    <xsl:template match="MedlineCitation/CommentsCorrectionsList/CommentsCorrections[@RefType='Cites']/PMID[@Version='1']" mode="seealsorelations">
        <xsl:variable name="citation_doc" select="document(concat($article_path,.))"/>  
        <xsl:variable name="citation_doi" select="$citation_doc/PubmedArticleSet/PubmedArticle/PubmedData/ArticleIdList/ArticleId[@IdType='doi']"/>
        <xsl:if test="string-length($citation_doi)>0">
            <rdf:Description>
                <xsl:attribute name="rdf:about" select="concat($doi_prefix,$citation_doi)"/>
                <rdfs:seeAlso>
                    <xsl:attribute name="rdf:resource" select="concat($pubmed_prefix,.)"/>                                    
                </rdfs:seeAlso>
            </rdf:Description>
        </xsl:if>
    </xsl:template>                 
        
    <xsl:template match="MedlineCitation/Article">
        <xsl:apply-templates select="PublicationTypeList"/>
        <xsl:for-each select="Journal/ISSN">
            <dcterms:isPartOf>
                <xsl:attribute name="rdf:resource" select="concat($issn_prefix,.)"/>
            </dcterms:isPartOf>
        </xsl:for-each>
        <dcterms:title><xsl:value-of select="ArticleTitle"/></dcterms:title>

        <xsl:apply-templates select="AuthorList"/>
        
        <dcterms:issued>
            <xsl:value-of select="concat(Journal/JournalIssue/PubDate/Year,'-',Journal/JournalIssue/PubDate/Month)"/>
            <xsl:if test="string-length(Journal/JournalIssue/PubDate/Day)>0">
                <xsl:value-of select="concat('-',Journal/JournalIssue/PubDate/Day)"/>
            </xsl:if>
        </dcterms:issued>
        <xsl:if test="string-length(Journal/JournalIssue/Volume)>0">
            <bibo:volume>
                <xsl:value-of select="Journal/JournalIssue/Volume"/>
            </bibo:volume>
        </xsl:if>
        <xsl:if test="string-length(Journal/JournalIssue/Issue)>0">
            <bibo:issue>
                <xsl:value-of select="Journal/JournalIssue/Issue"/>
            </bibo:issue>
        </xsl:if>
        
    </xsl:template>
    
    <xsl:template match="PublicationTypeList">       
        
        <rdf:type>
            <xsl:choose>
                <xsl:when test="string-length(PublicationType[@UI='D016428'])>0">
                    <xsl:attribute name="rdf:resource"
                        >http://purl.org/spar/fabio/JournalArticle</xsl:attribute>
                </xsl:when>
                <xsl:when test="string-length(PublicationType[@UI='D016422'])>0">
                    <xsl:attribute name="rdf:resource"
                        >http://purl.org/spar/fabio/Letter</xsl:attribute>
                </xsl:when>
                <xsl:when test="string-length(PublicationType[@UI='D016421'])>0">
                    <xsl:attribute name="rdf:resource"
                        >http://purl.org/spar/fabio/Editorial</xsl:attribute>
                </xsl:when>
                <xsl:when test="string-length(PublicationType[@UI='D016433'])>0">
                    <xsl:attribute name="rdf:resource"
                        >http://purl.org/spar/fabio/NewsItem</xsl:attribute>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:attribute name="rdf:resource"
                        >http://purl.org/dc/dcmitype/Text</xsl:attribute>
                </xsl:otherwise>
            </xsl:choose>
        </rdf:type>
    </xsl:template>
    
    <xsl:template match="MedlineCitations/CommentsCorrectionsList">        
        <xsl:for-each select="CommentsCorrections[@RefType='Cites']/PMID[@Version='1']">
            <cito:cites>
                <xsl:attribute name="rdf:resource" select="concat($pubmed_prefix,.)"/>
            </cito:cites>
        </xsl:for-each>
    </xsl:template>
 
    
    
    
    
</xsl:stylesheet>
