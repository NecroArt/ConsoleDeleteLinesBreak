package main;
	class ShowProperties
	{
	    public static void main(String[] args)
	    {
	    	
	    	System.out.println(System.getProperty("os.name"));
	        System.getProperties().list(System.out);
	    }
	}
