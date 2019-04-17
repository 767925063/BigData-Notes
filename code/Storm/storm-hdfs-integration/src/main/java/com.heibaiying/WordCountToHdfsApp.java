package com.heibaiying;

import com.heibaiying.component.DataSourceSpout;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.AuthorizationException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.hdfs.bolt.HdfsBolt;
import org.apache.storm.hdfs.bolt.format.DefaultFileNameFormat;
import org.apache.storm.hdfs.bolt.format.DelimitedRecordFormat;
import org.apache.storm.hdfs.bolt.format.FileNameFormat;
import org.apache.storm.hdfs.bolt.format.RecordFormat;
import org.apache.storm.hdfs.bolt.rotation.FileRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy;
import org.apache.storm.hdfs.bolt.rotation.FileSizeRotationPolicy.Units;
import org.apache.storm.hdfs.bolt.sync.CountSyncPolicy;
import org.apache.storm.hdfs.bolt.sync.SyncPolicy;
import org.apache.storm.topology.TopologyBuilder;

/**
 * 进行词频统计 并将统计结果存储到HDFS中
 * <p>
 * hdfs://hadoopp001:8020 path
 */
public class WordCountToHdfsApp {

    private static final String DATA_SOURCE_SPOUT = "dataSourceSpout";
    private static final String HDFS_BOLT = "hdfsBolt";

    public static void main(String[] args) {

        // 定义存储文本的分隔符
        RecordFormat format = new DelimitedRecordFormat()
                .withFieldDelimiter("|");

        // 同步策略: 每100个tuples之后就会把数据从缓存刷新到HDFS中
        SyncPolicy syncPolicy = new CountSyncPolicy(100);

        // 文件策略: 每个文件大小上限1M,超过限定时,创建新文件并继续写入
        FileRotationPolicy rotationPolicy = new FileSizeRotationPolicy(1.0f, Units.MB);

        // 定义完整路径
        FileNameFormat fileNameFormat = new DefaultFileNameFormat()
                .withPath("/storm-hdfs/");

        // 定义HdfsBolt
        HdfsBolt hdfsBolt = new HdfsBolt()
                .withFsUrl("hdfs://hadoop001:8020")
                .withFileNameFormat(fileNameFormat)
                .withRecordFormat(format)
                .withRotationPolicy(rotationPolicy)
                .withSyncPolicy(syncPolicy);


        // 构建Topology
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout(DATA_SOURCE_SPOUT, new DataSourceSpout());
        // save to HBase
        builder.setBolt(HDFS_BOLT, hdfsBolt, 1).shuffleGrouping(DATA_SOURCE_SPOUT);


        // 如果外部传参cluster则代表线上环境启动,否则代表本地启动
        if (args.length > 0 && args[0].equals("cluster")) {
            try {
                StormSubmitter.submitTopology("ClusterWordCountToHdfsApp", new Config(), builder.createTopology());
            } catch (AlreadyAliveException | InvalidTopologyException | AuthorizationException e) {
                e.printStackTrace();
            }
        } else {
            LocalCluster cluster = new LocalCluster();
            cluster.submitTopology("LocalWordCountToHdfsApp",
                    new Config(), builder.createTopology());
        }
    }
}