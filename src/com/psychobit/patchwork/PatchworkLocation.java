package com.psychobit.patchwork;

import java.io.Serializable;

import org.bukkit.Location;

/**
 * Custom object for representing locations that can be serialized
 * @author Bit
 */
public class PatchworkLocation implements Serializable
{
	private static final long serialVersionUID = -6469239102525197602L;
	private String world;
	private double x;
	private double y;
	private double z;

	public PatchworkLocation( Location location )
	{
		this.world = location.getWorld().getName();
		this.x= location.getX();
		this.y = location.getY();
		this.z = location.getZ();
	}
	
	@Override
	public int hashCode()
	{
	    Double hashcode = ( x + y + z ) * x * y * z; 
	    return hashcode.hashCode() + world.hashCode();
	}
	
	@Override
	public boolean equals( Object that )
	{
		if ( that instanceof Location )
		{
			Location location = (Location) that;
			if ( !location.getWorld().getName().equals( this.world ) ) return false;
			if ( location.getX() != this.x ) return false;
			if ( location.getY() != this.y ) return false;
			if ( location.getZ() != this.z ) return false;
			return true;
		}
		if ( that instanceof PatchworkLocation )
		{
			PatchworkLocation location = (PatchworkLocation) that;
			if ( !location.getWorld().equals( this.world ) ) return false;
			if ( location.getX() != this.x ) return false;
			if ( location.getY() != this.y ) return false;
			if ( location.getZ() != this.z ) return false;
			return true;
		}
		return false;
	}

	private double getZ()
	{
		return this.z;
	}

	private double getY()
	{
		return this.y;
	}

	private double getX()
	{
		return this.x;
	}

	private String getWorld()
	{
		return this.world;
	}
	
}
