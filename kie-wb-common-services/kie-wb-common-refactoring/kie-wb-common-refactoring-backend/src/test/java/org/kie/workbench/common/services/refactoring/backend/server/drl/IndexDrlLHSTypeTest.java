/*
 * Copyright 2014 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.services.refactoring.backend.server.drl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.junit.Test;
import org.kie.workbench.common.services.refactoring.backend.server.BaseIndexingTest;
import org.kie.workbench.common.services.refactoring.backend.server.TestIndexer;
import org.kie.workbench.common.services.refactoring.backend.server.indexing.RuleAttributeNameAnalyzer;
import org.kie.workbench.common.services.refactoring.backend.server.query.QueryBuilder;
import org.kie.workbench.common.services.refactoring.model.index.terms.RuleAttributeIndexTerm;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueTypeIndexTerm;
import org.uberfire.ext.metadata.backend.lucene.index.LuceneIndex;
import org.uberfire.ext.metadata.backend.lucene.util.KObjectUtil;
import org.uberfire.ext.metadata.engine.Index;
import org.uberfire.ext.metadata.model.KObject;
import org.uberfire.java.nio.file.Path;

import static org.apache.lucene.util.Version.*;
import static org.junit.Assert.*;

public class IndexDrlLHSTypeTest extends BaseIndexingTest<TestDrlFileTypeDefinition> {

    @Test
    public void testIndexDrlLHSType() throws IOException, InterruptedException {
        //Add test files
        final Path path1 = basePath.resolve( "drl1.drl" );
        final String drl1 = loadText( "drl1.drl" );
        ioService().write( path1,
                           drl1 );
        final Path path2 = basePath.resolve( "drl2.drl" );
        final String drl2 = loadText( "drl2.drl" );
        ioService().write( path2,
                           drl2 );
        final Path path3 = basePath.resolve( "drl3.drl" );
        final String drl3 = loadText( "drl3.drl" );
        ioService().write( path3,
                           drl3 );

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index

        final Index index = getConfig().getIndexManager().get( org.uberfire.ext.metadata.io.KObjectUtil.toKCluster( basePath.getFileSystem() ) );

        //Check type extraction (with wildcards)
        {
            final IndexSearcher searcher = ( (LuceneIndex) index ).nrtSearcher();
            final TopScoreDocCollector collector = TopScoreDocCollector.create( 10,
                                                                                true );
            final Query query = new QueryBuilder().useWildcards().addTerm( new ValueTypeIndexTerm( "*.Applicant" ) ).build();

            searcher.search( query,
                             collector );
            final ScoreDoc[] hits = collector.topDocs().scoreDocs;
            assertEquals( 3,
                          hits.length );

            final List<KObject> results = new ArrayList<KObject>();
            for ( int i = 0; i < hits.length; i++ ) {
                results.add( KObjectUtil.toKObject( searcher.doc( hits[ i ].doc ) ) );
            }
            assertContains( results,
                            path1 );
            assertContains( results,
                            path2 );
            assertContains( results,
                            path3 );

            ( (LuceneIndex) index ).nrtRelease( searcher );
        }

        //Check type extraction (without wildcards)
        {
            final IndexSearcher searcher = ( (LuceneIndex) index ).nrtSearcher();
            final TopScoreDocCollector collector = TopScoreDocCollector.create( 10,
                                                                                true );
            final Query query = new QueryBuilder().addTerm( new ValueTypeIndexTerm( "org.kie.workbench.common.services.refactoring.backend.server.drl.classes.Applicant" ) ).build();

            searcher.search( query,
                             collector );
            final ScoreDoc[] hits = collector.topDocs().scoreDocs;
            assertEquals( 3,
                          hits.length );

            final List<KObject> results = new ArrayList<KObject>();
            for ( int i = 0; i < hits.length; i++ ) {
                results.add( KObjectUtil.toKObject( searcher.doc( hits[ i ].doc ) ) );
            }
            assertContains( results,
                            path1 );
            assertContains( results,
                            path2 );
            assertContains( results,
                            path3 );

            ( (LuceneIndex) index ).nrtRelease( searcher );
        }

        //Check type extraction (with wildcards)
        {
            final IndexSearcher searcher = ( (LuceneIndex) index ).nrtSearcher();
            final TopScoreDocCollector collector = TopScoreDocCollector.create( 10,
                                                                                true );
            final Query query = new QueryBuilder().useWildcards().addTerm( new ValueTypeIndexTerm( "*.Mortgage" ) ).build();

            searcher.search( query,
                             collector );
            final ScoreDoc[] hits = collector.topDocs().scoreDocs;
            assertEquals( 2,
                          hits.length );

            final List<KObject> results = new ArrayList<KObject>();
            for ( int i = 0; i < hits.length; i++ ) {
                results.add( KObjectUtil.toKObject( searcher.doc( hits[ i ].doc ) ) );
            }
            assertContains( results,
                            path2 );
            assertContains( results,
                            path3 );

            ( (LuceneIndex) index ).nrtRelease( searcher );
        }

        //Check type extraction (without wildcards)
        {
            final IndexSearcher searcher = ( (LuceneIndex) index ).nrtSearcher();
            final TopScoreDocCollector collector = TopScoreDocCollector.create( 10,
                                                                                true );
            final Query query = new QueryBuilder().addTerm( new ValueTypeIndexTerm( "org.kie.workbench.common.services.refactoring.backend.server.drl.classes.Mortgage" ) ).build();

            searcher.search( query,
                             collector );
            final ScoreDoc[] hits = collector.topDocs().scoreDocs;
            assertEquals( 2,
                          hits.length );

            final List<KObject> results = new ArrayList<KObject>();
            for ( int i = 0; i < hits.length; i++ ) {
                results.add( KObjectUtil.toKObject( searcher.doc( hits[ i ].doc ) ) );
            }
            assertContains( results,
                            path2 );
            assertContains( results,
                            path3 );

            ( (LuceneIndex) index ).nrtRelease( searcher );
        }

    }

    @Override
    protected TestIndexer getIndexer() {
        return new TestDrlFileIndexer();
    }

    @Override
    public Map<String, Analyzer> getAnalyzers() {
        return new HashMap<String, Analyzer>() {{
            put( RuleAttributeIndexTerm.TERM,
                 new RuleAttributeNameAnalyzer( LUCENE_40 ) );
        }};
    }

    @Override
    protected TestDrlFileTypeDefinition getResourceTypeDefinition() {
        return new TestDrlFileTypeDefinition();
    }

    @Override
    protected String getRepositoryName() {
        return this.getClass().getSimpleName();
    }

}
