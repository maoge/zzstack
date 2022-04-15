package dbtest;

import java.sql.Array;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.zzstack.paas.underlying.dbclient.CRUD;
import com.zzstack.paas.underlying.dbclient.LoadBalancedDBSrcPool;
import com.zzstack.paas.underlying.dbclient.SqlBean;
import com.zzstack.paas.underlying.dbclient.exception.DBException;

public class ClickHouseTest {

    private static final String INSERT_SQL = "insert into test(user_id, create_date, update_count) values(?, ?, ?)";
    // private static final String UPDATE_SQL = "ALTER TABLE test UPDATE update_count = ? WHERE user_id = ?";
    private static final String UPSERT_SQL = "insert into test(user_id, create_date, update_count) select user_id, create_date, ? from test where user_id = ?";
    private static final String SELECT_SQL = "select user_id, create_date, update_count from test";
    
    private static final String SELECT_NESTED = "select abstract.slice_id, abstract.slice_type, abstract.slice_content from dt_paper";
    private static final String INSERT_NESTED = "INSERT INTO dt_paper("
                                                   + "doc_id,"
                                                   + "source,"
                                                   + "title_content,"
                                                   + "authors.name,"
                                                   + "authors.organization,"
                                                   + "authors.email,"
                                                   + "abstract_id,"
                                                   + "abstract_title,"
                                                   + "abstract.slice_id,"
                                                   + "abstract.slice_type,"
                                                   + "abstract.slice_content,"
                                                   + "introduction_id,"
                                                   + "introduction_title,"
                                                   + "introduction.slice_id,"
                                                   + "introduction.slice_type,"
                                                   + "introduction.slice_content,"
                                                   + "results_id,"
                                                   + "results_title,"
                                                   + "results.slice_id,"
                                                   + "results.slice_type,"
                                                   + "results.slice_content,"
                                                   + "discussion_id,"
                                                   + "discussion_title,"
                                                   + "discussion.slice_id,"
                                                   + "discussion.slice_type,"
                                                   + "discussion.slice_content,"
                                                   + "methods_id,"
                                                   + "methods_title,"
                                                   + "methods.slice_id,"
                                                   + "methods.slice_type,"
                                                   + "methods.slice_content,"
                                                   + "reference_id,"
                                                   + "reference_title,"
                                                   + "reference_item.slice_content"
                                                   + ") format Values ("
                                                   + "?, ?, ?, ?, ?, ?, "
                                                   + "?, ?, ?, ?, ?, "
                                                   + "?, ?, ?, ?, ?, "
                                                   + "?, ?, ?, ?, ?, "
                                                   + "?, ?, ?, ?, ?, "
                                                   + "?, ?, ?, ?, ?, "
                                                   + "?, ?, ?"
                                                   + ")";

    /*
    CREATE TABLE smsdb.test\
    (\
        user_id             UInt64,\
        create_date         Date DEFAULT toDate(now()),\
        update_count        UInt8 DEFAULT 0\
    ) ENGINE = ReplacingMergeTree()\
    PARTITION BY toYYYYMMDD(create_date)\
    ORDER BY (user_id)\
    primary key (user_id);
    */

    public static void main(String[] args) {
        LoadBalancedDBSrcPool.get("clickhouse");

        testInsert();
        testUpdate();
        testSelect();
        
        testNestedInsert();
        testNestedSelect();
    }
    
    private static String genUUID() {
        UUID uuid = UUID.randomUUID();
        return uuid.toString();
    }
    
    private static void testNestedInsert() {
        CRUD c = new CRUD("clickhouse");
        
        SqlBean sqlBean1 = new SqlBean(INSERT_NESTED);
        sqlBean1.putParam(new Long(1));
        sqlBean1.putParam("https://doi.org/10.1038/s41467-022-29443-w");
        sqlBean1.putParam("Learning meaningful representations of protein sequences");
        sqlBean1.putParam(new String[] {"Nicki Skafte Detlefsen", "Søren Hauberg", "Wouter Boomsma"});
        sqlBean1.putParam(new String[] {"", "", ""});
        sqlBean1.putParam(new String[] {"", "", ""});

        sqlBean1.putParam(genUUID());
        sqlBean1.putParam("Abstract");
        sqlBean1.putParam(new String[] {genUUID(), genUUID(), genUUID()});
        sqlBean1.putParam(new Integer[] {1, 1, 1});
        sqlBean1.putParam(new String[] {
                "How we choose to represent our data has a fundamental impact on our ability to subsequently extract information from them. Machine learning promises to automatically determine efficient representations from large unstructured datasets, such as those arising in biology. However, empirical evidence suggests that seemingly minor changes to these machine learning models yield drastically different data representations that result in different biological interpretations of data. ", 
                "This begs the question of what even constitutes the most meaningful representation. Here, we approach this question for representations of protein sequences, which have received considerable attention in the recent literature. We explore two key contexts in which representations naturally arise: transfer learning and interpretable learning. ", 
                "In the first context, we demonstrate that several contemporary practices yield suboptimal performance, and in the latter we demonstrate that taking representation geometry into account significantly improves interpretability and lets the models reveal biological information that is otherwise obscured."
            });

        sqlBean1.putParam(genUUID());
        sqlBean1.putParam("Introduction");
        sqlBean1.putParam(new String[] {genUUID(), genUUID(), genUUID()});
        sqlBean1.putParam(new Integer[] {1, 1, 1});
        sqlBean1.putParam(new String[] {
                "Data representations play a crucial role in the statistical analysis of biological data. At its core, a representation is a distillation of raw data into an abstract, high-level and often lower-dimensional space that captures the essential features of the original data. This can subsequently be used for data exploration, e.g. through visualization, or task-specific predictions where limited data is available.",
                "Given the importance of representations it is no surprise that we see a rise in biology of representation learning1, a subfield of machine learning where the representation is estimated alongside the statistical model. In the analysis of protein sequences in particular, the last years have produced a number of studies that demonstrate how representations can help extract important biological information automatically from the millions of observations acquired through modern sequencing technologies2,3,4,5,6,7,8,9,10,11,12,13,14.",
                "While these promising results indicate that learned representations can have substantial impact on scientific data analysis, they also beg the question: what is a good representation? This elementary question is the focus of this paper."
            });

        sqlBean1.putParam(genUUID());
        sqlBean1.putParam("Results");
        sqlBean1.putParam(new String[] {genUUID(), genUUID(), genUUID()});
        sqlBean1.putParam(new Integer[] {1, 1, 1});
        sqlBean1.putParam(new String[] {
                "Representation learning has at least two uses: In transfer learning we seek a representation that improves a downstream task, and in data interpretation the representation should reveal the data’s underlying patterns, e.g. through visualization. Since the first has been at the center of recent literature4,5,8,9,10,20,21, we place our initial focus there, and turn later to data interpretation.",
                "Representations for transfer learning Transfer learning addresses the problems caused by limited access to labeled data. For instance, when predicting the stability of a given protein, we only have limited training data available as it is experimentally costly to measure stability. The key idea is to leverage the many available unlabeled protein sequences to learn (pre-train) a general protein representation through an embedding model, and then train a problem-specific task model on top using the limited labeled training data (Fig. 1).",
                "In the protein setting, learning representations for transfer learning can be implemented at different scopes. It can be addressed at a universal scope, where representations are learned to reflect general properties of all proteins, or it can be implemented at the scope of an individual protein family, where an embedding model is pre-trained only on closely related sequences. Initially, we will focus on universal setting, but will return to family-specific models in the second half of the paper."
            });

        sqlBean1.putParam(genUUID());
        sqlBean1.putParam("Discussion");
        sqlBean1.putParam(new String[] {genUUID(), genUUID(), genUUID()});
        sqlBean1.putParam(new Integer[] {1, 1, 1});
        sqlBean1.putParam(new String[] {
                "Learned representations of protein sequences can substantially improve systems for making biological predictions, and may also help to reveal previously uncovered biological information. In this paper, we have illuminated parts of the answer to the question of what constitutes a meaningful representation of proteins. One of the conclusions is that the question itself does not have a single general answer, and must always be qualified with a specification of the purpose of the representation. A representation that is suitable for making predictions may not be optimal for a human investigator to better understand the underlying biology, and vice versa. The enticing idea of a single protein representation for all tasks thus seems unworkable in practice.",
                "Designing purposeful representations Designing a representation for a given task requires reflection over which biological properties we wish the representation to encapsulate. Different biological aspects of a protein will place different demands on the representations, but it is not straightforward to enforce specific properties in a representation. We can, however, steer the representation learning by (1) picking appropriate model architectures, (2) preprocessing the data, (3) choosing suitable objective functions, and (4) placing prior distributions on parts of the model. We discuss each of these in turn.",
                "Informed network architectures can be difficult to construct as the usual neural network ‘building blocks’ are fairly elementary mathematical functions that are not immediately linked to high-level biological information. Nonetheless, our discussion of length-invariant sequence representations is a simple example of how one might inform the model architecture of the biology of the task. It is generally acknowledged that global protein properties are not linearly related to local properties. It is therefore not surprising when we show that the model performance significantly improves when we allow the model to learn such a nonlinear relationship instead of relying on the common linear average of local representations. It would be interesting to push this idea beyond the Resnet architecture that we explored here, in particular in combination with the recent large-scale transformer-based language models. We speculate that while similar ‘low-hanging fruit’ may remain in currently applied network architectures, they are limited, and more advanced tools are needed to encode biological information into network architectures. The internal representations in attention-based architectures have been shown to recover known physical interactions between proteins37,38, opening the door to the incorporation of prior information about known physical interactions in a protein. Recent work on permutation and rotation invariance/equivariance in neural networks49,50 hold promise, though they have yet to be explored exhaustively in representation learning."
            });

        sqlBean1.putParam(genUUID());
        sqlBean1.putParam("Methods");
        sqlBean1.putParam(new String[] {genUUID(), genUUID(), genUUID()});
        sqlBean1.putParam(new Integer[] {1, 1, 1});
        sqlBean1.putParam(new String[] {
                "Variational autoencoders",
                "A variational autoencoder assumes that data X is generated from some (unknown) latent factors Z though the process pθ(X∣Z). The latent variables Z can be viewed as the compressed representation of X. Latent space models try to model the joint distribution of X and Z as pθ(X, Z) = pθ(Z)pθ(X∣Z). ",
                "The generating process can then be viewed as a two-step procedure: first a latent variable Z is sampled from the prior and then data X is sampled from the conditional pθ(X∣Z) (often called the decoder). Since X is discrete by nature, pθ(X∣Z) is modeled as a Categorical distribution pθ(X∣Z) ~ Cat(C, lθ(Z)) with C classes and lθ(Z) being the log-probabilities for each class. To make the model flexible enough to capture higher-order amino acid interactions, we model lθ(Z) as a neural network. Even though data X is discrete, we use continuous latent variables Z ~ N(0, 1)."
            });
        
        sqlBean1.putParam(genUUID());
        sqlBean1.putParam("References");
        sqlBean1.putParam(new String[] {
                "Bengio, Y., Courville, A. & Vincent, P. Representation Learning: A Review and New Perspectives. IEEE Trans. Pattern Anal. Mach. Intell. 35, 1798–1828 (2013).",
                "Riesselman, A. J., Ingraham, J. B. & Marks, D. S. Deep generative models of genetic variation capture the effects of mutations. Nat. Methods 15, 816–822 (2018).",
                "Bepler, T. & Berger, B. Learning protein sequence embeddings using information from structure. In International Conference on Learning Representations (2019)."
            });
        
        c.putSqlBean(sqlBean1);
        
        if (c.executeUpdate()) {
            System.out.println("nested insert OK ......");
        } else {
            System.err.println("nested insert NOK ......");
        }
    }
    
    private static void testNestedSelect() {
        SqlBean sqlBean = new SqlBean(SELECT_NESTED);

        CRUD c = new CRUD("clickhouse");
        c.putSqlBean(sqlBean);
        try {
            List<HashMap<String, Object>> result = c.queryForList();
            if (result != null && !result.isEmpty()) {
                for (HashMap<String, Object> map : result) {
                    Set<Entry<String, Object>> entrySet = map.entrySet();
                    for (Entry<String, Object> entry : entrySet) {
                        Object val = entry.getValue();
                        Class<?> clazz = val.getClass();
                        String type = clazz.getSimpleName();
                        if (val instanceof Array) {
                            Array arr = (Array) val;
                            String baseTypeName = arr.getBaseTypeName();
                            Object data = arr.getArray();
                            Object[] arrData = (Object[]) data;
                            
                            printArray(entry.getKey(), type, baseTypeName, arrData);
                        } else {
                            printObject(entry.getKey(), type, val);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static void printObject(String key, String type, Object obj) {
        String info = String.format("%s | %s | %s", key, type, obj.toString());
        System.out.println(info);
    }
    
    private static void printArray(String key, String type, String baseTypeName, Object[] arrData) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < arrData.length; ++i) {
            if (i > 0) {
                sb.append("|");
            }
            sb.append(arrData[i].toString());
        }
        
        String info = String.format("%s | %s | %s | %s", key, type, baseTypeName, sb.toString());
        System.out.println(info);
    }
    
    private static void testInsert() {
        CRUD c = new CRUD("clickhouse");
        java.sql.Timestamp ts = new java.sql.Timestamp(System.currentTimeMillis());
        
        SqlBean sqlBean1 = new SqlBean(INSERT_SQL);
        sqlBean1.addParams(new Object[] { 1000, ts, 0 });
        c.putSqlBean(sqlBean1);
        
        SqlBean sqlBean2 = new SqlBean(INSERT_SQL);
        sqlBean2.addParams(new Object[] { 1001, ts, 0 });
        c.putSqlBean(sqlBean2);
        
        c.batchUpdate();
    }
    
    private static void testUpdate() {
        CRUD c = new CRUD("clickhouse");
        
        SqlBean sqlBean1 = new SqlBean(UPSERT_SQL);
        sqlBean1.addParams(new Object[] { 3, 1000 });
        c.putSqlBean(sqlBean1);
        c.executeUpdate();
        
        SqlBean sqlBean2 = new SqlBean(UPSERT_SQL);
        sqlBean2.addParams(new Object[] { 3, 1001 });
        c.putSqlBean(sqlBean2);
        c.executeUpdate();
        
        // c.batchUpdate();
    }
    
    private static void testSelect() {
        SqlBean sqlBean = new SqlBean(SELECT_SQL);

        CRUD c = new CRUD("clickhouse");
        c.putSqlBean(sqlBean);
        try {
            List<HashMap<String, Object>> result = c.queryForList();
            if (result != null && !result.isEmpty()) {
                for (HashMap<String, Object> map : result) {
                    Set<Entry<String, Object>> entrySet = map.entrySet();
                    for (Entry<String, Object> entry : entrySet) {
                        Object val = entry.getValue();
                        String type = val.getClass().getName();
                        String info = String.format("%s | %s | %s", entry.getKey(), type, val.toString());
                        System.out.println(info);
                    }
                }
            }
        } catch (DBException e) {
            e.printStackTrace();
        }
    }

}
