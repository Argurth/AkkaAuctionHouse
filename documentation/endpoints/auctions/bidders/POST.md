# Add a bidder to an auction

## Description

This endpoint allow you to add a bidder to an auction

## Authentication

No authentication required

## Path

<code>POST</code> /auctions/:item/bidders

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
	"bidderName": "testuser"
}
```

- bidderName must be present and must be a string, the bidderName provided should not have already been added to the auction

## Success response

In order for the request to be successful, the :item field in the url must correspond to an already created option with an "Opened" state.

In which case the server will answer:

- Code: <code>201 CREATED</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content: 
    ```
    {
    	"name": "testuser"
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

- Code: <code>404 NOT FOUND</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content:
    ```
    {
    	"message": "The auction 'test2' doesn't exist"
    }
    ```
- When:
    - The item parameter in route does not match an existing auction
    
OR

- Code: <code>422 UNPROCESSABLE ENTITY LOCKED</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content:
    ```
    {
    	"message": "'testuser' already joined this auction"
    }
    ```
- When:
    - The bidderName parameter contained a bidder name already added to this auction
    
OR

- Code: <code>423 LOCKED</code>
- Headers:
    ```
    Content-type: application/json
    ```
- Content:
    ```
    {
    	"message": "The actual auction state 'Closed' doesn't allow this action"
    }
    ```
- When:
    - The state of the auction is not "Opened"