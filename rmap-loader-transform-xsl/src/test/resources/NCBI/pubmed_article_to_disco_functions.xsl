<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema"
    xmlns:fn="http://example.com/fn/"
    exclude-result-prefixes="xs"
    version="2.0">
    
    <xsl:variable name="anzctr_prefix" select="'http://www.anzctr.org.au/'"/>
    <xsl:variable name="bioproject_prefix" select="'http://www.ncbi.nlm.nih.gov/bioproject/'"/>
    <xsl:variable name="pdb_prefix" select="'http://dx.doi.org/10.2210/pdb'"/>
    <xsl:variable name="who_trial_prefix" select="'http://apps.who.int/trialsearch/Trial2.aspx?TrialID='"/>
    <xsl:variable name="clinicaltrialsgov_prefix" select="'https://clinicaltrials.gov/show/'"/>
    <xsl:variable name="dbgap_prefix" select="'http://www.ncbi.nlm.nih.gov/projects/gap/cgi-bin/study.cgi?study_id='"/>
    <xsl:variable name="dryad_prefix" select="'http://dx.doi.org/10.5061/dryad.'"/>
    <xsl:variable name="doipath_prefix" select="'http://dx.doi.org/'"/>
    <xsl:variable name="drks_prefix" select="'http://www.drks.de/'"/>
    <xsl:variable name="genbank_prefix" select="'http://www.ncbi.nlm.nih.gov/nuccore/'"/>
    <xsl:variable name="geo_prefix" select="'http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc='"/>
    <xsl:variable name="isrctn_prefix" select="'http://www.isrctn.com/'"/>
    <xsl:variable name="ntr_prefix" select="'http://www.trialregister.nl/trialreg/admin/rctview.asp?TC='"/>
    <xsl:variable name="omim_prefix" select="'http://omim.org/entry/'"/>
    <xsl:variable name="pactr_prefix" select="'http://www.pactr.org/ATMWeb/appmanager/atm/atmregistry?dar=true&amp;tNo='"/>
    <xsl:variable name="pubchem_bioassay_prefix" select="'http://pubchem.ncbi.nlm.nih.gov/assay/assay.cgi?aid='"/>
    <xsl:variable name="pubchem_compound_prefix" select="'http://pubchem.ncbi.nlm.nih.gov/compound/'"/>
    <xsl:variable name="pubchem_substance_prefix" select="'http://pubchem.ncbi.nlm.nih.gov/substance/'"/>
    <xsl:variable name="rebec_prefix" select="'http://www.ensaiosclinicos.gov.br/rg/'"/>
    <xsl:variable name="rpcec_prefix" select="'http://rpcec.sld.cu/en/trials/'"/>
    <xsl:variable name="uniprotkb_prefix" select="'http://www.uniprot.org/uniprot/'"/>
    
    <xsl:function name="fn:buildAccessUrl">
        <xsl:param name="databank"/>
        <xsl:param name="accessionNumber"/>
        <!-- NOTE: any of the rows commented out will return the original accession number 
            and go into the database as a blank node-->
        <xsl:choose>
            
            <xsl:when test="$databank eq 'ANZCTR'">
                <xsl:value-of select="concat($anzctr_prefix,$accessionNumber,'.aspx')"/>
            </xsl:when>
            <xsl:when test="$databank eq 'BioProject'">
                <xsl:value-of select="concat($bioproject_prefix,substring($accessionNumber,6))"/>
            </xsl:when>
            <xsl:when test="$databank eq 'ChiCTR'">
                <xsl:value-of select="concat($who_trial_prefix,$accessionNumber)"/>                
            </xsl:when>
            <xsl:when test="$databank eq 'ClinicalTrials.gov'">
                <xsl:value-of select="concat($clinicaltrialsgov_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'CRiS'">
                <xsl:value-of select="concat($who_trial_prefix,$accessionNumber)"/>  
            </xsl:when>
            <xsl:when test="$databank eq 'CTRI'">
                <xsl:value-of select="concat($who_trial_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'dbGaP'">
                <xsl:value-of select="concat($dbgap_prefix,$accessionNumber)"/>
            </xsl:when>
            <!-- can't resolve
            <xsl:when test="$databank eq 'dbSNP'">
                <xsl:value-of select="concat($bioproject_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'dbVar'">
                <xsl:value-of select="concat($bioproject_prefix,$accessionNumber)"/>
            </xsl:when>
            -->
            <xsl:when test="$databank eq 'Dryad'">
                <xsl:value-of select="concat($dryad_prefix,lower-case($accessionNumber))"/>
            </xsl:when>
            <xsl:when test="$databank eq 'DRKS'">
                <xsl:value-of select="concat($drks_prefix,$accessionNumber)"/>
            </xsl:when>
            <!-- can't resolve
            <xsl:when test="$databank eq 'EudraCT'">
                <xsl:value-of select="concat($bioproject_prefix,$accessionNumber))"/>
            </xsl:when>
            -->
            <xsl:when test="$databank eq 'figshare'">
                <xsl:value-of select="concat($doipath_prefix,$accessionNumber)"/>
            </xsl:when>
            <!-- can't resolve
            <xsl:when test="$databank eq 'GDB'">
                <xsl:value-of select="concat($bioproject_prefix,substring($accessionNumber,6))"/>
            </xsl:when>
            -->
            <xsl:when test="$databank eq 'GENBANK'">
                <xsl:value-of select="concat($genbank_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'GEO'">
                <xsl:value-of select="concat($geo_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'IRCT'">
                <xsl:value-of select="concat($who_trial_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'ISRCTN'">
                <xsl:value-of select="concat($isrctn_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'JPRN'">
                <xsl:value-of select="concat($who_trial_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'NTR'">
                <xsl:value-of select="concat($ntr_prefix,substring($accessionNumber,4))"/>
            </xsl:when>
            <xsl:when test="$databank eq 'OMIM'">
                <xsl:value-of select="concat($omim_prefix,substring($accessionNumber,5))"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PACTR'">
                <xsl:value-of select="concat($pactr_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PDB'">
                <xsl:value-of select="concat($pdb_prefix,lower-case($accessionNumber),'/pdb')"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PIR'">
                <xsl:value-of select="concat($genbank_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PubChem-BioAssay'">
                <xsl:value-of select="concat($pubchem_bioassay_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PubChem-Compound'">
                <xsl:value-of select="concat($pubchem_compound_prefix,substring($accessionNumber,6))"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PubChem-Substance'">
                <xsl:value-of select="concat($pubchem_substance_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'ReBec'">
                <xsl:value-of select="concat($rebec_prefix,'RBR-',lower-case(substring($accessionNumber,4)))"/>
            </xsl:when>
            <xsl:when test="$databank eq 'RefSeq'">
                <xsl:value-of select="concat($genbank_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'RPCEC'">
                <xsl:value-of select="concat($rpcec_prefix,$accessionNumber,'-En')"/>
            </xsl:when>
            <xsl:when test="$databank eq 'SLCTR'">
                <xsl:if test="starts-with($accessionNumber,'SLCTR')">
                    <xsl:value-of select="concat($who_trial_prefix,$accessionNumber)"/>
                </xsl:if>
                <xsl:if test="not(starts-with($accessionNumber,'SLCTR'))">
                    <xsl:value-of select="concat($who_trial_prefix,'SLCTR/',$accessionNumber)"/>                   
                </xsl:if>
            </xsl:when>
            <!-- link formation not consistent, so leave it as just an idenfier
            <xsl:when test="$databank eq 'SRA'">
                <xsl:value-of select="concat($bioproject_prefix,substring($accessionNumber,6))"/>
            </xsl:when>
             -->
            <xsl:when test="$databank eq 'SWISSPROT'">
                <xsl:value-of select="concat($uniprotkb_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:when test="$databank eq 'TCTR'">
                <xsl:if test="starts-with($accessionNumber,'TCTR')">
                    <xsl:value-of select="concat($who_trial_prefix,$accessionNumber)"/>
                </xsl:if>
                <xsl:if test="not(starts-with($accessionNumber,'TCTR'))">
                    <xsl:value-of select="concat($who_trial_prefix,'TCTR',$accessionNumber)"/>                    
                </xsl:if>
            </xsl:when>
            <!-- cant find an example of these
            <xsl:when test="$databank eq 'UniMES'">
                <xsl:value-of select="concat($bioproject_prefix,substring($accessionNumber,6))"/>
            </xsl:when>
            <xsl:when test="$databank eq 'UniParc'">
                <xsl:value-of select="concat($bioproject_prefix,substring($accessionNumber,6))"/>
            </xsl:when>
            <xsl:when test="$databank eq 'UniRef'">
                <xsl:value-of select="concat($bioproject_prefix,substring($accessionNumber,6))"/>
            </xsl:when>-->
            <xsl:when test="$databank eq 'UniProtKB'">
                <xsl:value-of select="concat($uniprotkb_prefix,$accessionNumber)"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="$accessionNumber"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
    
    
    <xsl:function name="fn:repositoryUrl">
        <xsl:param name="databank"/>
        <xsl:choose>
            
            <xsl:when test="$databank eq 'ANZCTR'">
                <xsl:value-of select="'http://www.anzctr.org.au'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'BioProject'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/bioproject/'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'ChiCTR'">
                <xsl:value-of select="'http://www.chictr.org'"/>                
            </xsl:when>
            <xsl:when test="$databank eq 'ClinicalTrials.gov'">
                <xsl:value-of select="'https://www.clinicaltrials.gov'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'CRiS'">
                <xsl:value-of select="'https://cris.nih.go.kr'"/>  
            </xsl:when>
            <xsl:when test="$databank eq 'CTRI'">
                <xsl:value-of select="'http://ctri.nic.in'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'dbGaP'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/gap'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'dbSNP'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/snp'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'dbVar'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/dbvar'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'Dryad'">
                <xsl:value-of select="'http://datadryad.org'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'DRKS'">
                <xsl:value-of select="'https://drks-neu.uniklinik-freiburg.de/drks_web/'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'EudraCT'">
                <xsl:value-of select="'https://eudract.ema.europa.eu'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'figshare'">
                <xsl:value-of select="'http://figshare.com/'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'GDB'">
                <xsl:value-of select="'http://dbpedia.org/resource/GDB_Human_Genome_Database'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'GENBANK'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/genbank'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'GEO'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/geo/'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'IRCT'">
                <xsl:value-of select="'http://www.irct.ir'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'ISRCTN'">
                <xsl:value-of select="'http://www.isrctn.com'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'JPRN'">
                <xsl:value-of select="'http://www.jprn.org'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'NTR'">
                <xsl:value-of select="'http://www.trialregister.nl'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'OMIM'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/omim'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PACTR'">
                <xsl:value-of select="'http://www.pactr.org/'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PDB'">
                <xsl:value-of select="'http://www.rcsb.org/pdb'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PIR'">
                <xsl:value-of select="'http://pir.georgetown.edu/'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PubChem-BioAssay'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/pcassay'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PubChem-Compound'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/pccompound'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'PubChem-Substance'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/pcsubstance'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'ReBec'">
                <xsl:value-of select="'http://www.ensaiosclinicos.gov.br'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'RefSeq'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/refseq'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'RPCEC'">
                <xsl:value-of select="'http://registroclinico.sld.cu'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'SLCTR'">
                <xsl:value-of select="'http://slctr.lk'"/>                   
            </xsl:when>
            <xsl:when test="$databank eq 'SRA'">
                <xsl:value-of select="'http://www.ncbi.nlm.nih.gov/sra'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'SWISSPROT'">
                <xsl:value-of select="'http://www.uniprot.org'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'TCTR'">
                <xsl:value-of select="'http://www.clinicaltrials.in.th'"/>                    
            </xsl:when>
            <xsl:when test="$databank eq 'UniMES'">
                <xsl:value-of select="'http://www.uniprot.org/help/unimes'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'UniParc'">
                <xsl:value-of select="'http://www.uniprot.org/uniparc'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'UniProtKB'">
                <xsl:value-of select="'http://www.uniprot.org/uniprot'"/>
            </xsl:when>
            <xsl:when test="$databank eq 'UniRef'">
                <xsl:value-of select="'http://www.uniprot.org/uniref'"/>
            </xsl:when>
            <xsl:otherwise>
                <xsl:value-of select="''"/>
            </xsl:otherwise>
        </xsl:choose>
    </xsl:function>
        
</xsl:stylesheet>