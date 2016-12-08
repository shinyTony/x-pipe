package com.ctrip.xpipe.redis.meta.server.keeper.keepermaster.impl;

import java.util.concurrent.ScheduledExecutorService;

import org.unidal.tuple.Pair;

import com.ctrip.xpipe.pool.XpipeNettyClientKeyedObjectPool;
import com.ctrip.xpipe.redis.core.metaserver.MetaServerMultiDcServiceManager;
import com.ctrip.xpipe.redis.meta.server.config.MetaServerConfig;
import com.ctrip.xpipe.redis.meta.server.keeper.keepermaster.KeeperMasterChooserAlgorithm;
import com.ctrip.xpipe.redis.meta.server.meta.CurrentMetaManager;
import com.ctrip.xpipe.redis.meta.server.meta.DcMetaCache;

/**
 * @author wenchao.meng
 *
 * Nov 4, 2016
 */
public class DefaultDcKeeperMasterChooser extends AbstractKeeperMasterChooser{

	private MetaServerMultiDcServiceManager metaServerMultiDcServiceManager;
	
	private MetaServerConfig metaServerConfig;
	
	private XpipeNettyClientKeyedObjectPool keyedObjectPool;
	
	private KeeperMasterChooserAlgorithm keeperMasterChooserAlgorithm;

	public DefaultDcKeeperMasterChooser(String clusterId, String shardId, MetaServerConfig metaServerConfig, MetaServerMultiDcServiceManager metaServerMultiDcServiceManager, 
			DcMetaCache dcMetaCache, CurrentMetaManager currentMetaManager, ScheduledExecutorService scheduled, XpipeNettyClientKeyedObjectPool keyedObjectPool) {
		this(clusterId, shardId, metaServerConfig, metaServerMultiDcServiceManager, dcMetaCache, currentMetaManager, scheduled, keyedObjectPool, DEFAULT_KEEPER_MASTER_CHECK_INTERVAL_SECONDS);
	}

	public DefaultDcKeeperMasterChooser(String clusterId, String shardId, MetaServerConfig metaServerConfig, MetaServerMultiDcServiceManager metaServerMultiDcServiceManager,
			DcMetaCache dcMetaCache, CurrentMetaManager currentMetaManager, ScheduledExecutorService scheduled, XpipeNettyClientKeyedObjectPool keyedObjectPool, int checkIntervalSeconds) {
		super(clusterId, shardId, dcMetaCache, currentMetaManager, scheduled, checkIntervalSeconds);
		this.metaServerConfig = metaServerConfig;
		this.keyedObjectPool = keyedObjectPool;
		this.metaServerMultiDcServiceManager = metaServerMultiDcServiceManager;
	}

	@Override
	protected Pair<String, Integer> chooseKeeperMaster() {
		
		if(dcMetaCache.isCurrentDcPrimary(clusterId, shardId)){
			
			if(keeperMasterChooserAlgorithm == null || keeperMasterChooserAlgorithm instanceof BackupDcKeeperMasterChooserAlgorithm){
				
				logger.info("[chooseKeeperMaster][current dc become primary, change algorithm]{}, {}", clusterId, shardId);
				keeperMasterChooserAlgorithm = new PrimaryDcKeeperMasterChooserAlgorithm(clusterId, shardId, dcMetaCache, currentMetaManager, keyedObjectPool, checkIntervalSeconds/2);
			}
		}else{
			if(keeperMasterChooserAlgorithm == null || keeperMasterChooserAlgorithm instanceof PrimaryDcKeeperMasterChooserAlgorithm){
				logger.info("[chooseKeeperMaster][current dc become backup, change algorithm]{}, {}", clusterId, shardId);
				keeperMasterChooserAlgorithm = new BackupDcKeeperMasterChooserAlgorithm(clusterId, shardId, dcMetaCache, currentMetaManager, metaServerConfig, metaServerMultiDcServiceManager);
			}
		}
		return keeperMasterChooserAlgorithm.choose();
	}
	
	protected KeeperMasterChooserAlgorithm getKeeperMasterChooserAlgorithm() {
		return keeperMasterChooserAlgorithm;
	}

}
