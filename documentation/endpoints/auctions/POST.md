# Create an auction

## Description

This endpoint allow you to create a new auction.

## Authentication

No authentication required

## Path

<code>POST</code> /auctions

## Headers

```
Content-type: application/json
```

## Url parameters

This endpoint does not accept url parameters.

## Request body

This endpoint must have a well-formatted json body:
```
{
	"item": "test",
	"incrementPolicy": {
		"key": "MinimalIncrement",
		"min": 10
	},
	"startingPrice": 200,
	"startDate": 1553239485000,
	"endDate": 1563239485000
}
```

- item must be present and must be a string
- incrementPolicy must be present
    - key must be present and correspond to an existing increment policy key ("FreeIncrement", "MinimalIncrement")
    - min must be present only if the increment policy key is "MinimalIncrement"
- startingPrice must be present and must be a positive integer representing the starting price in cents
- startDate must be present and must be a unix timestamp expressed in milliseconds
- endDate must be present and must be a unix timestamp expressed in milliseconds

## Success response

In order for the request to be successful, the item must not have been already created by another request.

In which case the server will answer:

- Code: <code>201 CREATED</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content: 
    ```
    {
    	"auctionState": "Planned",
    	"endDate": 1563239485000,
    	"bids": [],
    	"startingPrice": 200,
    	"incrementPolicy": {
    		"key": "MinimalIncrement",
    		"min": 10
    	},
    	"startDate": 1553239485000,
    	"bidders": [],
    	"item": "test"
    }
    ```
    
## Error responses

If the request is unsuccessful, the server may responds with the following errors:

- Code: <code>400 BAD REQUEST</code>
- Headers:
    ```
    Content-type: text/plain; charset=UTF-8
    ```
- Content:
    ```
    The request content was malformed: [A descriptive message]
    ```
- When:
    - The request was malformed
    
OR

- Code: <code>422 UNPROCESSABLE ENTITY</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content:
    ```
    {
    	"message": "The provided starting price '-200' is negative"
    }
    ```
- When:
    - The startingPrice parameter was negative
    
OR

- Code: <code>422 UNPROCESSABLE ENTITY</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content:
    ```
    {
    	"message": "The auction 'test' already exist"
    }
    ```
- When:
    - The item parameter had already been created by another request