<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" 
    xmlns:ore="http://www.openarchives.org/ore/terms/" 
    xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
    xmlns:cito="http://purl.org/spar/cito/" 
    xmlns:mets="http://www.loc.gov/METS/"
    xmlns:dim="http://www.dspace.org/xmlns/dspace/dim" 
    xmlns:dcterms="http://purl.org/dc/terms/"  
    xmlns:fn="http://example.com/fn/"
    xmlns:dc="http://purl.org/dc/elements/1.1/"
    xmlns:prov="http://www.w3.org/ns/prov#"
    exclude-result-prefixes="xs"
    version="2.0">
    
    <!-- protein databank prefixes -->
    <xsl:param name="pdb_webpage_prefix" select="'http://www.rcsb.org/pdb/explore/explore.do?pdbId='"/>
    <xsl:param name="bioproject_api_path" select="'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=bioproject&amp;retmode=xml&amp;id='"/>
    <xsl:param name="clinicaltrialsgov_api_path" select="'https://clinicaltrials.gov/show/XXXXXXXXXX?displayxml=true'"/>
    <xsl:param name="pdb_api_path" select="'http://www.rcsb.org/pdb/rest/describePDB?structureId='"/>
    <xsl:param name="dryad_api_path" select="'http://datadryad.org/resource/doi:10.5061/dryad.XXXXXXXXXX/mets.xml'"/>
    <xsl:param name="genbank_api_path" select="'http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=genbank&amp;retmode=xml&amp;id='"/>
    
    <xsl:include href="pubmed_article_to_disco_functions.xsl"/>
    
    <xsl:template match="DataBank" mode="databank_links">
        <xsl:variable name="databank" select="DataBankName"/>
        <xsl:for-each select="AccessionNumberList/AccessionNumber">
            <xsl:variable name="i" select="position()"/>
            <xsl:variable name="accessUrl" select="fn:buildAccessUrl($databank, .)"/>
            <ore:aggregates>  
                <xsl:if test="not($accessUrl eq .)">
                    <xsl:attribute name="rdf:resource" select="$accessUrl"/>  
                </xsl:if>
                <!-- blank node when there is no URL for accession -->
                <xsl:if test="$accessUrl eq .">
                    <xsl:attribute name="rdf:nodeID" select="concat('dataset_',$i)"/>
                </xsl:if>
            </ore:aggregates>
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template match="DataBank" mode="dbcites">
        <xsl:variable name="databank" select="DataBankName"/>
        <xsl:for-each select="AccessionNumberList/AccessionNumber">
            <xsl:variable name="i" select="position()"/>
            <xsl:variable name="accessUrl" select="fn:buildAccessUrl($databank, .)"/>
            <cito:cites>        
                <xsl:if test="not($accessUrl eq .)">
                    <xsl:attribute name="rdf:resource" select="$accessUrl"/>  
                </xsl:if>
                <!-- blank node when there is no URL for accession -->
                <xsl:if test="$accessUrl eq .">
                    <xsl:attribute name="rdf:nodeID" select="concat('dataset_',$i)"/>
                </xsl:if>
            </cito:cites>
        </xsl:for-each>
    </xsl:template>
    
    <xsl:template match="DataBank" mode="dbdetails">
        <xsl:param name="main_article_id"/>
        <xsl:variable name="databank" select="DataBankName"/>
        <xsl:for-each select="AccessionNumberList/AccessionNumber">
            <xsl:variable name="i" select="position()"/>
            <!--clinical trials registries that have no API for further info-->
            <xsl:if test="$databank eq 'ANZCTR'
                        or $databank eq 'ChiCTR'
                        or $databank eq 'CRiS'
                        or $databank eq 'CTRI'
                        or $databank eq 'DRKS'
                        or $databank eq 'EudraCT'
                        or $databank eq 'IRCT'
                        or $databank eq 'ISRCTN'
                        or $databank eq 'JPRN'
                        or $databank eq 'NTR'
                        or $databank eq 'PACTR'
                        or $databank eq 'ReBec'
                        or $databank eq 'RPCEC'
                        or $databank eq 'SLCTR'
                        or $databank eq 'TCTR'">
                <xsl:apply-templates select="." mode="genericclinicaltrial">
                    <xsl:with-param name="main_article_id" select="$main_article_id"/>    
                    <xsl:with-param name="databank" select="$databank"/>       
                    <xsl:with-param name="accessionPos" select="$i"/>             
                </xsl:apply-templates>
            </xsl:if>
                        
            <!--molecular databank links that have no API for further info-->
            <xsl:if test="$databank eq 'dbGaP'
                            or $databank eq 'dbSNP'
                            or $databank eq 'dbVar'
                            or $databank eq 'GENBANK'
                            or $databank eq 'GEO'
                            or $databank eq 'PIR'
                            or $databank eq 'RefSeq'
                            or $databank eq 'SRA'
                            or $databank eq 'SWISSPROT'
                            or $databank eq 'UniMES'
                            or $databank eq 'UniParc'
                            or $databank eq 'UniProtKB'
                            or $databank eq 'UniRef'">
                <xsl:apply-templates select="." mode="genericmoleculardata">
                    <xsl:with-param name="main_article_id" select="$main_article_id"/>   
                    <xsl:with-param name="databank" select="$databank"/>     
                    <xsl:with-param name="accessionPos" select="$i"/>                              
                </xsl:apply-templates>
            </xsl:if>
            
            <!--databank links that have no API for further info-->
            <xsl:if test="$databank eq 'figshare'
                    or $databank eq 'OMIN'
                    or $databank eq 'PubChem-BioAssay'
                    or $databank eq 'PubChem-Compound'
                    or $databank eq 'PubChem-Substance'">
                <xsl:apply-templates select="." mode="genericdataset">
                    <xsl:with-param name="main_article_id" select="$main_article_id"/>     
                    <xsl:with-param name="databank" select="$databank"/>    
                    <xsl:with-param name="accessionPos" select="$i"/>
                </xsl:apply-templates>
            </xsl:if>
            
            <xsl:if test="$databank eq 'BioProject'">
                <xsl:apply-templates select="." mode="bioproject">
                    <xsl:with-param name="main_article_id" select="$main_article_id"/>                    
                </xsl:apply-templates>
            </xsl:if>
            <xsl:if test="$databank eq 'ClinicalTrials.gov'">
                <xsl:apply-templates select="." mode="clinicaltrialsgov">
                    <xsl:with-param name="main_article_id" select="$main_article_id"/>      
                </xsl:apply-templates>
            </xsl:if>
            <xsl:if test="$databank eq 'Dryad'">
                <xsl:apply-templates select="." mode="dryad">
                    <xsl:with-param name="main_article_id" select="$main_article_id"/>      
                </xsl:apply-templates>
            </xsl:if>
            <xsl:if test="$databank eq 'GDB'">
                <xsl:apply-templates select="." mode="gdb">
                    <xsl:with-param name="main_article_id" select="$main_article_id"/>  
                    <xsl:with-param name="accessionPos" select="$i"/>    
                </xsl:apply-templates>
            </xsl:if>
            <xsl:if test="$databank eq 'PDB'">
                <xsl:apply-templates select="." mode="pdb">
                    <xsl:with-param name="main_article_id" select="$main_article_id"/>                    
                </xsl:apply-templates>
            </xsl:if>
        </xsl:for-each>
    </xsl:template>    
    
    <xsl:template match="AccessionNumber" mode="genericdataset">
        <xsl:param name="main_article_id"/>
        <xsl:param name="databank"/>
        <xsl:param name="accessionPos"/>
        <xsl:variable name="accessionNumber" select="."/>
        <xsl:variable name="accessUrl" select="fn:buildAccessUrl($databank, .)"/>
        
        <rdf:Description>
            <xsl:if test="not($accessUrl eq $accessionNumber)">
                <xsl:attribute name="rdf:about" select="$accessUrl"/>  
            </xsl:if>
            
            <!-- blank node when there is no URL for accession -->
            <xsl:if test="$accessUrl eq $accessionNumber">
                <xsl:attribute name="rdf:nodeID" select="concat('dataset_',$accessionPos)"/>  
                <dc:identifier>
                    <xsl:value-of select="."/>
                </dc:identifier> 
            </xsl:if>       
            
            <rdf:type>    
                <xsl:attribute name="rdf:resource" select="'http://purl.org/spar/fabio/Dataset'"/>     
            </rdf:type>
            
            <dcterms:isPartOf>
                <xsl:attribute name="rdf:resource" select="fn:repositoryUrl($databank)"/>
            </dcterms:isPartOf>
            
            <cito:isCitedBy>
                <xsl:attribute name="rdf:resource" select="$main_article_id"/>
            </cito:isCitedBy>
        </rdf:Description>
    </xsl:template>
    
    <xsl:template match="AccessionNumber" mode="genericmoleculardata">
        <xsl:param name="main_article_id"/>
        <xsl:param name="databank"/>
        <xsl:param name="accessionPos"/>
        
        <xsl:variable name="accessionNumber" select="."/>
        <xsl:variable name="accessUrl" select="fn:buildAccessUrl($databank, .)"/>
        <rdf:Description>
            <xsl:if test="not($accessUrl eq $accessionNumber)">
                <xsl:attribute name="rdf:about" select="$accessUrl"/>  
            </xsl:if>
            
            <!-- blank node when there is no URL for accession -->
            <xsl:if test="$accessUrl eq $accessionNumber">
                <xsl:attribute name="rdf:nodeID" select="concat('dataset_',$accessionPos)"/>  
                <dc:identifier>
                    <xsl:value-of select="."/>
                </dc:identifier> 
            </xsl:if>   
            
            <rdf:type>    
                <xsl:attribute name="rdf:resource" select="'http://purl.org/spar/fabio/Dataset'"/>     
            </rdf:type>
            <rdf:type>
                <xsl:attribute name="rdf:resource" select="'http://purl.obolibrary.org/obo/ERO_0100354'"/> 
            </rdf:type>
                            
            <dcterms:isPartOf>
                <xsl:attribute name="rdf:resource" select="fn:repositoryUrl($databank)"/>
            </dcterms:isPartOf>
            <cito:isCitedBy>
                <xsl:attribute name="rdf:resource" select="$main_article_id"/>
            </cito:isCitedBy>
        </rdf:Description>
    </xsl:template>
        
    
    <xsl:template match="AccessionNumber" mode="genericclinicaltrial">
        <xsl:param name="main_article_id"/>
        <xsl:param name="databank"/>
        <xsl:param name="accessionPos"/>
        
        <xsl:variable name="accessionNumber" select="."/>
        <xsl:variable name="accessUrl" select="fn:buildAccessUrl($databank, .)"/>
        <rdf:Description>
            <xsl:if test="not($accessUrl eq $accessionNumber)">
                <xsl:attribute name="rdf:about" select="$accessUrl"/>  
            </xsl:if>
            
            <!-- blank node when there is no URL for accession -->
            <xsl:if test="$accessUrl eq $accessionNumber">
                <xsl:attribute name="rdf:nodeID" select="concat('dataset_',$accessionPos)"/>  
                <dc:identifier>
                    <xsl:value-of select="."/>
                </dc:identifier> 
            </xsl:if>   
            <rdf:type>    
                <xsl:attribute name="rdf:resource" select="'http://purl.obolibrary.org/obo/ERO_0000016'"/>     
            </rdf:type>
            <rdf:type>
                <xsl:attribute name="rdf:resource" select="'http://www.w3.org/ns/prov#Activity'"/>
            </rdf:type>
            <dcterms:isPartOf>
                <xsl:attribute name="rdf:resource" select="fn:repositoryUrl($databank)"/>
            </dcterms:isPartOf>
            <cito:isCitedBy>
                <xsl:attribute name="rdf:resource" select="$main_article_id"/>
            </cito:isCitedBy>
            <dc:identifier>
                <xsl:value-of select="."/>
            </dc:identifier>
        </rdf:Description>
    </xsl:template>
    
    <xsl:template match="AccessionNumber" mode="bioproject">
        <xsl:param name="main_article_id"/>
        <xsl:variable name="accessionNumber" select="."/>
        <rdf:Description>
            <xsl:attribute name="rdf:about" select="fn:buildAccessUrl('BioProject', .)"/>
            <rdf:type>    
                <xsl:attribute name="rdf:resource" select="'http://purl.org/spar/fabio/Dataset'"/>     
            </rdf:type>
            <cito:isCitedBy>
                <xsl:attribute name="rdf:resource" select="$main_article_id"/>
            </cito:isCitedBy>
            <dc:identifier>
                <xsl:value-of select="."/>
            </dc:identifier>
            
            <dcterms:isPartOf>
                <xsl:attribute name="rdf:resource" select="fn:repositoryUrl('BioProject')"/>
            </dcterms:isPartOf>
            
            <xsl:variable name="bioproject_doc" select="document(concat($bioproject_api_path,$accessionNumber))"/>           
            
            <dcterms:title><xsl:value-of 
                select="$bioproject_doc/RecordSet/DocumentSummary/Project/ProjectDescr/Title"/></dcterms:title> 

            <dcterms:issued rdf:datatype="http://www.w3.org/2001/XMLSchema#date">
                <xsl:value-of select="$bioproject_doc/RecordSet/DocumentSummary/Submission/@submitted"/>
            </dcterms:issued>
            <dcterms:modified rdf:datatype="http://www.w3.org/2001/XMLSchema#date">
                <xsl:value-of select="$bioproject_doc/RecordSet/DocumentSummary/Submission/@last_update"/>
            </dcterms:modified>
            
        </rdf:Description>
    </xsl:template>
    
    <xsl:template match="AccessionNumber" mode="clinicaltrialsgov">
        <xsl:param name="main_article_id"/>
        <xsl:variable name="accessionNumber" select="."/>
        <rdf:Description>
            <xsl:attribute name="rdf:about" select="fn:buildAccessUrl('ClinicalTrials.gov', .)"/>
            <rdf:type>    
                <xsl:attribute name="rdf:resource" select="'http://purl.obolibrary.org/obo/ERO_0000016'"/>     
            </rdf:type>
            <rdf:type>
                <xsl:attribute name="rdf:resource" select="'http://www.w3.org/ns/prov#Activity'"/>
            </rdf:type>
            <cito:isCitedBy>
                <xsl:attribute name="rdf:resource" select="$main_article_id"/>
            </cito:isCitedBy>
            <dc:identifier>
                <xsl:value-of select="."/>
            </dc:identifier>
            <dcterms:isPartOf>
                <xsl:attribute name="rdf:resource" select="fn:repositoryUrl('ClinicalTrials.gov')"/>
            </dcterms:isPartOf>
            
            <xsl:variable name="clinicaltrialsgov_doc" select="document(replace($clinicaltrialsgov_api_path,'XXXXXXXXXX',$accessionNumber))"/>           
            
            <dcterms:title><xsl:value-of 
                select="$clinicaltrialsgov_doc/clinical_study/official_title"/></dcterms:title> 
            
            <xsl:variable name="startdate" select="$clinicaltrialsgov_doc/clinical_study/start_date"/>
            <xsl:variable name="enddate" select="$clinicaltrialsgov_doc/clinical_study/completion_date"/>
            <xsl:variable name="enddate_type" select="$clinicaltrialsgov_doc/clinical_study/completion_date/@type"/>
            <dcterms:temporal>
                <xsl:value-of select="$startdate"/>
                <xsl:if test="string-length($enddate)>0">
                    <xsl:value-of select="concat(' to ',$enddate)"/>
                    <xsl:if test="string-length($enddate_type)>0">
                        <xsl:value-of select="concat(' (',$enddate_type,')')"/>
                    </xsl:if>
                </xsl:if>
            </dcterms:temporal>
        </rdf:Description>
    </xsl:template>
    
    <xsl:template match="AccessionNumber" mode="dryad">
        <xsl:param name="main_article_id"/>
        <xsl:variable name="accessionNumber" select="."/>
        <rdf:Description>
            <xsl:attribute name="rdf:about" select="fn:buildAccessUrl('Dryad', .)"/>
            <rdf:type>    
                <xsl:attribute name="rdf:resource" select="'http://purl.org/spar/fabio/Dataset'"/>     
            </rdf:type>
            <dcterms:isPartOf>
                <xsl:attribute name="rdf:resource" select="fn:repositoryUrl('Dryad')"/>
            </dcterms:isPartOf>
            
            <cito:isCitedBy>
                <xsl:attribute name="rdf:resource" select="$main_article_id"/>
            </cito:isCitedBy>
            
            <xsl:variable name="dryad_doc" select="document(replace($dryad_api_path,'XXXXXXXXXX',lower-case($accessionNumber)))"/>           
            
            <xsl:variable name="alturi" 
                select="$dryad_doc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='identifier'][@qualifier='uri'][@mdschema='dc']"/>
            <xsl:if test="string-length($alturi)>0 and contains($alturi,'handle')">
                <rdfs:seeAlso>
                    <xsl:attribute name="rdf:resource" 
                        select="$alturi"/>
                </rdfs:seeAlso>
            </xsl:if>
            <dcterms:title><xsl:value-of 
                select="$dryad_doc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='title'][@mdschema='dc']"/></dcterms:title> 
            
            <xsl:variable name="issueddate" select="$dryad_doc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='date'][@qualifier='issued']"/>
            <dcterms:issued>
                <xsl:if test="string-length($issueddate) eq 10">
                    <xsl:attribute name="rdf:datatype" select="'http://www.w3.org/2001/XMLSchema#date'"/>
                </xsl:if>
                <xsl:if test="string-length($issueddate) eq 20">
                    <xsl:attribute name="rdf:datatype" select="'http://www.w3.org/2001/XMLSchema#datetime'"/>
                </xsl:if>
                <xsl:value-of  select="$issueddate"/>
            </dcterms:issued>
            
            <xsl:variable name="availdate" select="$dryad_doc/mets:METS/mets:dmdSec/mets:mdWrap/mets:xmlData/dim:dim/dim:field[@element='date'][@qualifier='available']"/>
            <dcterms:available>
                <xsl:if test="string-length($availdate) eq 10">
                    <xsl:attribute name="rdf:datatype" select="'http://www.w3.org/2001/XMLSchema#date'"/>
                </xsl:if>
                <xsl:if test="string-length($availdate) eq 20">
                    <xsl:attribute name="rdf:datatype" select="'http://www.w3.org/2001/XMLSchema#datetime'"/>
                </xsl:if>
                <xsl:value-of select="$availdate"/>
            </dcterms:available>

        </rdf:Description>
    </xsl:template>
        
    <xsl:template match="AccessionNumber" mode="gdb">
        <xsl:param name="main_article_id"/>
        <xsl:param name="accessionPos"/>
        <xsl:variable name="accessionNumber" select="."/>
        <rdf:Description>
            <xsl:attribute name="rdf:nodeID" select="concat('dataset_',$accessionPos)"/>
            <rdf:type>    
                <xsl:attribute name="rdf:resource" select="'http://purl.org/spar/fabio/Dataset'"/>     
            </rdf:type>
            <rdf:type>
                <xsl:attribute name="rdf:resource" select="'http://purl.obolibrary.org/obo/ERO_0100354'"/> 
            </rdf:type>
            <cito:isCitedBy>
                <xsl:attribute name="rdf:resource" select="$main_article_id"/>
            </cito:isCitedBy>
            <dc:identifier>
                <xsl:value-of select="."/>
            </dc:identifier>
            <dcterms:isPartOf>
                <xsl:attribute name="rdf:resource" select="fn:repositoryUrl('GDB')"/>
            </dcterms:isPartOf>
        </rdf:Description>
        <rdf:Description>
            <xsl:attribute name="rdf:resource" select="fn:repositoryUrl('GDB')"/>
            <rdfs:seeAlso>
                <xsl:attribute name="rdf:resource" select="'http://web.archive.org/web/19970605132915/http:/www.gdb.org/'"/>
            </rdfs:seeAlso>
        </rdf:Description>
    </xsl:template>
    
    <xsl:template match="AccessionNumber" mode="pdb">
        <xsl:param name="main_article_id"/>
        <xsl:variable name="accessionNumber" select="."/>
        <rdf:Description>
            <xsl:attribute name="rdf:about" select="fn:buildAccessUrl('PDB', .)"/>
            <rdf:type>    
                <xsl:attribute name="rdf:resource" select="'http://purl.org/spar/fabio/Dataset'"/>     
            </rdf:type>
            <rdf:type>
                <xsl:attribute name="rdf:resource" select="'http://purl.obolibrary.org/obo/ERO_0100354'"/> 
            </rdf:type>
            <cito:isCitedBy>
                <xsl:attribute name="rdf:resource" select="$main_article_id"/>
            </cito:isCitedBy>
            
            <dcterms:isPartOf>
                <xsl:attribute name="rdf:resource" select="fn:repositoryUrl('PDB')"/>
            </dcterms:isPartOf>
            
            <rdfs:seeAlso>
                <xsl:attribute name="rdf:resource">
                    <xsl:value-of select="concat($pdb_webpage_prefix,lower-case($accessionNumber))"/>
                </xsl:attribute>
            </rdfs:seeAlso>
            <dcterms:identifier>
                <xsl:attribute name="rdf:resource" select="concat('PDB:',$accessionNumber)"/>
            </dcterms:identifier>
            
            <xsl:variable name="pdb_doc" select="document(concat($pdb_api_path,$accessionNumber))"/>           
            
            <dcterms:title><xsl:value-of select="$pdb_doc/PDBdescription/PDB/@title"/></dcterms:title> 
            <dcterms:issued rdf:datatype="http://www.w3.org/2001/XMLSchema#date">
                <xsl:value-of select="$pdb_doc/PDBdescription/PDB/@release_date"/>
            </dcterms:issued>
            <dcterms:created rdf:datatype="http://www.w3.org/2001/XMLSchema#date">
                <xsl:value-of select="$pdb_doc/PDBdescription/PDB/@deposition_date"/>
            </dcterms:created>
            <dcterms:modified rdf:datatype="http://www.w3.org/2001/XMLSchema#date">
                <xsl:value-of select="$pdb_doc/PDBdescription/PDB/@last_modification_date"/>
            </dcterms:modified>
        </rdf:Description>
    </xsl:template>
    
    
</xsl:stylesheet>