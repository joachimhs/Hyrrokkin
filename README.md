Hyrrokkin
=========

Hyrrokkin is a Java library to convert POJOs into a JSON feed that Ember Data can consume via its RESTAdapter. The name Hyrrokkin comes from Norse and means "Fire-smoked". Hyrokkin is also a dark, shrivelled giantess that helped get Balders ship Hringhorni rolled out to sea. (http://en.wikipedia.org/wiki/Hyrrokkin)

In its first version, Hyrrokkin will support translating any POJO (Plain Old Java Object) into a JSON that the RESTAdapter understands. This means that it will flatten the object-tree and side-load any embedded data types into the JSON. 

Currently Hyrrokkin uses the GSON library in the background. This dependency might be removed in the future, making Hyrrokkin completely self-contained. 

Building up the Data
--------------------

Usually, models on the server might have fields that you do not want to send over the wire from the server to the client. Hyrrokkin expects you to annotate any fields/properties that you want to expose to the client with the @Expose annotation. In addition, it is possible to change the name of a property using the @SerializedName, while it is possible to override the rootKey of a class via the @SerializedClassName

Consider the following two class definitions: 

    @SerializedClassName("session")
    public class Session {
        @Expose
        @SerializedName("id")
        private String uuid;

        @Expose
        private User user;

        @Expose
        private boolean authenticated;

        @Expose
        private Long lastAccessed;

        @Expose
        private List<User> users;
    
        private boolean isLoaded;
    }
	
    @SerializedClassName("user")
    public class MySystemUserUser extends User {
        @Expose private Integer id;
        @Expose private String email;
        @Expose private Integer groupId;
        @Expose private Date createdAt;
        @Expose private User user;
    }
	
As you can see from the class above, we have marked the properties that we want to expose to the client side with the @Expose annotation.  This means that the property "isLoaded" wont be included when the object is serialized into JSON. Additionally, we have also maked the uuid-property of the Session class with the @SerializedName annotation. This means that the uuid property will become serialized into the "id"-property in the resulting JSON. 

Notice also, that we have overridden the root keys of both classes using the @SerializedClassName annotation

The resulting JSON, after serialization thus becomes: 

	{
	    "session": {
	        "id": "abcabvabv",
	        "user": "1",
	        "authenticated": true,
	        "lastAccessed": 1418254183981,
	        "users": [
	            45,
	            2
	        ]
	    }
		"users": [
	        {
	            "id": 2,
	            "email": "user email2",
	            "groupId": 6,
	            "createdAt": "2014-12-10T23:29:43Z"
	        },
	        {
	            "id": 1,
	            "email": "user email",
	            "groupId": 4,
	            "user": "2"
	        },
	        {
	            "id": 45,
	            "email": "user email4",
	            "groupId": 3
	        }
	    ]
	}
	
Is it production ready?
-----------------------

At this point, not really. It's definately usable, but the code needs to be cleaned up a bit and a few extra features needs to be added. I am personally using this serializer in my projects, but your mileage might vary depending on your requirements. 

Especially in order to support other serializing targets (like JSON API), a cleanup is needed. The code for Hyrrokkin is extracted from a larger project, and thus will be updated moving forward. 
	
How to use it
-------------

Using the serializer is quite straight forward. Simply instantiate a RestSerializer class and execite the serialize method. 

    Session session = repository.getSession(uuid);

    RestSerializer rs = new RestSerializer();
    JsonElement jsonElement = rs.serialize(session);

    if(jsonElement != null) {
        responseContent = jsonElement.toString();
    }

What about JSON API?
---------------------

There is a JSON API seraializer planned 

Tests
-----

While there are no tests committed just yet, the project does have tests. As Hyrrokkin is extracted from a larger project, test will be committed as soon as these can be extracted without also bringing the domain of that application with it :) 