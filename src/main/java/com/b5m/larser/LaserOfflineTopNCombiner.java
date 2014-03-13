package com.b5m.larser;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Reducer.Context;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.math.VectorWritable;

public class LaserOfflineTopNCombiner extends Reducer<IntDoublePairWritable, IntWritable, IntDoublePairWritable, IntWritable>{
	private int TOP_N;
	private int ITEM_DIMENSION;
	private int user;
	private int nTop;
	private Vector topNItem;
	
	protected void setup(Context context) throws IOException,
			InterruptedException {
		Configuration conf = context.getConfiguration();
		TOP_N = conf.getInt("laser.offline.topN.driver.top.n", 100);
		ITEM_DIMENSION = conf.getInt("laser.offline.topN.driver.item.dimension", 100000);

		user = -1;
		nTop = 0;
		topNItem = new RandomAccessSparseVector(ITEM_DIMENSION);
	}
	
	protected void reduce(IntDoublePairWritable key,
			Iterable<IntWritable> values, Context context) throws IOException,
			InterruptedException {
		int userId = key.getKey();
		double val = key.getValue();
		if (userId == this.user) {
			if(nTop >= TOP_N) {
				return;
			}
		}
		else {
			for (Element e : topNItem.nonZeroes()) {
				context.write(new IntDoublePairWritable(userId, e.get()), new IntWritable(e.index()));
			}
			this.user = userId;
			nTop = 0;
			topNItem.assign(0.0);
		}
		
		for (IntWritable item : values) {
			topNItem.set(item.get(), val);
			nTop ++;
			if (nTop >= TOP_N) {
				for (Element e : topNItem.nonZeroes()) {
					context.write(new IntDoublePairWritable(userId, e.get()), new IntWritable(e.index()));
				}
				topNItem.assign(0.0);
				return;
			}
		}
	}
}
