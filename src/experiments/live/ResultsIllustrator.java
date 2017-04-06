/*
 * Copyright (C) 2015 Evangelos Pournaras
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package experiments.live;

import enums.PSSMeasurementTags;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import protopeer.Experiment;
import protopeer.MainConfiguration;
import protopeer.measurement.MeasurementLog;
import protopeer.network.NetworkAddress;

/**
 *
 * @author Evangelos
 */
public class ResultsIllustrator {

    private Experiment experiment;
    private MeasurementLog mlog;
    
    public ResultsIllustrator(Experiment experiment){
        this.experiment=experiment;
        this.mlog=experiment.getRootMeasurementLog();
    }

    public void printEpochNumber(int epochNumber){
        System.out.print("Epoch: "+(int)(epochNumber*MainConfiguration.getSingleton().measurementEpochDuration/1000));
        System.out.print("\t");
    }

    public void printSumEpochMeasurement(int epochNumber, PSSMeasurementTags tag){
        System.out.print("Measurement-"+tag.toString()+": "+mlog.getAggregateByEpochNumber(epochNumber, tag).getSum());
        System.out.print("\t");
    }

    public void printEpochInDegreeStDev(int epochNumber){
        double sum=0.0;
        Set peerTags=mlog.getTagsOfType(NetworkAddress.class);
        for(Object tag:peerTags){
            sum=sum+mlog.getAggregateByEpochNumber(epochNumber, tag).getSum();
        }
        double average=sum/peerTags.size();
        sum=0.0;
        for(Object tag:peerTags){
            sum=sum+Math.pow((mlog.getAggregateByEpochNumber(epochNumber, tag).getSum()-average), 2.0);
        }
        double stdev=Math.sqrt(sum/peerTags.size());
        long l=Math.round(stdev*100);
        stdev=l/100.0;
        System.out.print("Measurement-Indegree St. Deviation: "+stdev);
    }

    public void printEpochInDegreeNodesProportion(int epochNumber){
        Map<Integer, Integer> proportions=new HashMap();
        Integer value=0;
        Set peerTags=mlog.getTagsOfType(NetworkAddress.class);
        for(Object tag:peerTags){
            value=(int)mlog.getAggregateByEpochNumber(epochNumber, tag).getSum();
            if(!proportions.containsKey(value)){
                proportions.put(value, new Integer(1));
            }
            else{
                proportions.put(value, new Integer(proportions.remove(value).intValue()+1));
            }
        }
        Integer test=new Integer(0);
        for(int i=0;i<300;i++){
            test=i;
            if(proportions.containsKey(i)){
                System.out.println(i+","+proportions.get(i).toString());
            }
        }
    }

    public void clearEpochLog(int epochNumber){
//        mlog.removeEpochLog(epochNumber);
    }

    public void printSumAggregateMeasurement(PSSMeasurementTags tag){
        System.out.println("Measurement-"+tag.toString()+": "+mlog.getAggregate(tag).getSum());
    }

}
