package software.aws.toolkits.eclipse.amazonq.util;

//import software.aws.toolkits.eclipse.amazonq.util.AwsRegion;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.regions.RegionMetadata;
import software.amazon.awssdk.regions.PartitionMetadata;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;

import static org.mockito.Mockito.*;



@TestMethodOrder(OrderAnnotation.class)
public class AwsRegionTest {
	
	private static Region mockRegion = mock(Region.class);
    private static RegionMetadata mockRegionMetadata = mock(RegionMetadata.class);
    private static PartitionMetadata mockPartitionMetadata = mock(PartitionMetadata.class);
    AwsRegion result;


    @BeforeAll 
    static void setUp() {	  	
        when(mockRegion.id())
        .thenReturn("us-west-1")    	//first test --> US West
        .thenReturn("ca-central-1")		//second test --> Canada
        .thenReturn("eu-north-1")  		//third test --> Europe
        .thenReturn("af-south-1") 		//fourth test --> Africa
        .thenReturn("ap-northeast-2")	//fifth test --> Asia Pacific
        .thenReturn("me-south-1") 		//sixth test --> Middle East
        .thenReturn("cn-north-1")		//seventh test --> China
        .thenReturn("us-east-1")		//eighth test --> US East
        .thenReturn("sa-east-1")		//ninth test --> South America
        .thenReturn("il-central-1");	//tenth test --> region code not outlined in AwsRegion.getCategory()
        
        when(mockRegion.metadata()).thenReturn(mockRegionMetadata);
        
        when(mockRegionMetadata.description())
        .thenReturn("US West (Oregon)")
        .thenReturn("Canada (Central)")
        .thenReturn("Europe (Stockholm)")
        .thenReturn("Africa (Cape Town)")
        .thenReturn("Asia Pacific (Seoul)") 
        .thenReturn("Middle East (Bahrain)")
        .thenReturn("China (Beijing)")
        .thenReturn("US East (N. Virginia)")
        .thenReturn("South America (Sao Paulo)")
        .thenReturn("Israel (Tel Aviv)"); //ensuring category is properly set for unlisted region code
        
        when(mockRegionMetadata.partition()).thenReturn(mockPartitionMetadata);
        when(mockPartitionMetadata.id()).thenReturn("aws");
    }

    //from() called before each test, which uses sequential .thenReturns above to set AwsRegion properties
    @BeforeEach
    void setUpEach() {
    	result = AwsRegion.from(mockRegion);
    }
    
    @Test
    @Order(1)
    public void testFromWithUsWestRegion() {   	
    	assertEquals("us-west-1", result.id());
        assertEquals("US West (Oregon)", result.name());
        assertEquals("aws", result.partitionId());
        assertEquals("North America", result.category());
        assertEquals("(Oregon) (us-west-1)", result.displayName());  
        
        //ensure results of object creation expected string format
        String toStringExpected = "{\"id\":\"us-west-1\",\"name\":\"US West (Oregon)\",\"partitionId\":\"aws\",\"category\":\"North America\",\"displayName\":\"(Oregon) (us-west-1)\"}";
        assertEquals(toStringExpected, result.toString());
    }
    
    @Test 
    @Order(2)
    public void testFromWithCanadaRegion() {
       assertEquals("ca-central-1", result.id());
       assertEquals("Canada (Central)", result.name());
       assertEquals("aws", result.partitionId());
       assertEquals("North America", result.category());
       assertEquals("Canada (Central) (ca-central-1)", result.displayName());
    }
    
    @Test 
    @Order(3)
    public void testFromWithEuropeRegion() {
	   assertEquals("eu-north-1", result.id());
       assertEquals("Europe (Stockholm)", result.name());
       assertEquals("aws", result.partitionId());
       assertEquals("Europe", result.category());
       assertEquals("(Stockholm) (eu-north-1)", result.displayName());
    }
    
    @Test 
    @Order(4)
    public void testFromWithAfricaRegion() {
        assertEquals("af-south-1", result.id());
       assertEquals("Africa (Cape Town)", result.name());
       assertEquals("aws", result.partitionId());
       assertEquals("Africa", result.category());
       assertEquals("Africa (af-south-1)", result.displayName());
    }
    
    @Test 
    @Order(5)
    public void testFromWithAsiaPacificRegion() {
        assertEquals("ap-northeast-2", result.id());
       assertEquals("Asia Pacific (Seoul)", result.name());
       assertEquals("aws", result.partitionId());
       assertEquals("Asia Pacific", result.category());
       assertEquals("Asia Pacific (ap-northeast-2)", result.displayName());
    }
    
    @Test 
    @Order(6)
    public void testFromWithMiddleEastRegion() {
        assertEquals("me-south-1", result.id());
       assertEquals("Middle East (Bahrain)", result.name());
       assertEquals("aws", result.partitionId());
       assertEquals("Middle East", result.category());
       assertEquals("Middle East (me-south-1)", result.displayName());
    }
    
    @Test 
    @Order(7)
    public void testFromWithChinaRegion() {
       assertEquals("cn-north-1", result.id());
       assertEquals("China (Beijing)", result.name());
       assertEquals("aws", result.partitionId());
       assertEquals("China", result.category());
       assertEquals("China (cn-north-1)", result.displayName());
    }
    
    @Test 
    @Order(8)
    public void testFromWithUSEastRegion() {
       assertEquals("us-east-1", result.id());
       assertEquals("US East (N. Virginia)", result.name());
       assertEquals("aws", result.partitionId());
       assertEquals("North America", result.category());
       assertEquals("(N. Virginia) (us-east-1)", result.displayName());
    }
    
    @Test 
    @Order(9)
    public void testFromWithSouthAmericaRegion() {
       assertEquals("sa-east-1", result.id());
       assertEquals("South America (Sao Paulo)", result.name());
       assertEquals("aws", result.partitionId());
       assertEquals("South America", result.category());
       assertEquals("South America (sa-east-1)", result.displayName());
    }
    
    @Test 
    @Order(10)
    public void testFromWithUnlistedRegion() {
       assertEquals("il-central-1", result.id());
       assertEquals("Israel (Tel Aviv)", result.name());
       assertEquals("aws", result.partitionId());
       assertNull(result.category());
       assertEquals("Israel (Tel Aviv)", result.displayName());
       
       //ensure expected string output given null value in category
       String toStringExpected = "{\"id\":\"il-central-1\",\"name\":\"Israel (Tel Aviv)\",\"partitionId\":\"aws\",\"category\":\"null\",\"displayName\":\"Israel (Tel Aviv)\"}";
       assertEquals(toStringExpected, result.toString());
    }
    
   
}
