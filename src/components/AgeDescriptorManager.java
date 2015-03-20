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
package components;

import enums.FingerDescriptorTypes;
import java.util.List;
import dsutil.protopeer.FingerDescriptor;

/**
 * This is a supportive class of the <code>ViewManager</code>. It defines 
 * methods that check and modify the age of finger descriptors.
 *
 * @author Evangelos
 */
public class AgeDescriptorManager {

    private FingerDescriptorTypes type;


    /**
     * Adds and age descriptor and initializes the age to zero.
     *
     * @param desciptor the managed finger descriptor
     */
    public void initAge(FingerDescriptor descriptor){
        descriptor.addDescriptor(type.AGE, 0.0);
    }
    
    /**
     * Increases the age of the descriptor by one
     *
     * @param desciptor the managed finger descriptor
     */
    public void increaseAge(FingerDescriptor descriptor){
        Double age=this.getAge(descriptor)+1.0;
        descriptor.replaceDescriptor(type.AGE, age);
    }

    /**
     * Increases the age of the descriptor by an amount of time
     *
     * @param desciptor the managed finger descriptor
     * @param passedTime the time passed for the age of the descriptor
     */
    public void increaseAge(FingerDescriptor descriptor, double passedTime){
        Double age=this.getAge(descriptor)+passedTime;
        descriptor.replaceDescriptor(type.AGE, age);
    }

    /**
     * Returns the age of a descriptor
     *
     * @param desciptor the descriptor whose age is requested
     */
    public double getAge(FingerDescriptor descriptor) {
        return (Double)descriptor.getDescriptor(this.type.AGE);
    }

    /**
     * It manually sets the value of the age descriptor
     *
     * @param desciptor the managed finger descriptor
     * @param age the age to set
     */
    public void setAge(FingerDescriptor descriptor, double age) {
        descriptor.replaceDescriptor(type.AGE, age);
    }

    /**
     * Checks if the age of a descriptor is older that the age of another
     * descriptor
     *
     * @param older the descriptor that is checked to be older
     * @param younger the descriptor that is checked to be younger
     */
    public boolean isOlder(FingerDescriptor older, FingerDescriptor younger){
        if(this.getAge(older)>this.getAge(younger)){
            return true;
        }
        return false;
    }


    /**
     * Checks if two descriptors a and b have the same age
     *
     * @param a a finger descriptor
     * @param b a finger descriptor
     */
    public boolean isSameAge(FingerDescriptor a, FingerDescriptor b){
        if(this.getAge(a)==this.getAge(b)){
            return true;
        }
        return false;
    }

    /**
     * Finds the oldest finger descriptor from a list of them
     *
     * @param neighbors the list of neighbors
     */
    public FingerDescriptor getOldestDescriptor(List<FingerDescriptor> neighbors){
        if(neighbors.size()>0){
            FingerDescriptor oldest=neighbors.get(0);//(int)(Math.random()*neighbors.size())
            for(FingerDescriptor neighbor:neighbors){
                if(isOlder(neighbor, oldest)){
                    oldest=neighbor;
                }
            }
            return oldest;
        }
        return null;
    }

}
