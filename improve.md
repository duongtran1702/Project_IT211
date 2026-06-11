#### Lấy dữ liệu 
```api
http://localhost:8080/api/v1/admin/users?keyword=Atmin&page=1&size=2
```
#### có dữ liệu trong db nhưng
```json
{
    "success": true,
    "message": "Users retrieved successfully",
    "data": {
        "content": [],
        "empty": true,
        "first": false,
        "last": true,
        "number": 1,
        "numberOfElements": 0,
        "pageable": {
            "offset": 2,
            "pageNumber": 1,
            "pageSize": 2,
            "paged": true,
            "sort": {
                "empty": true,
                "sorted": false,
                "unsorted": true
            },
            "unpaged": false
        },
        "size": 2,
        "sort": {
            "empty": true,
            "sorted": false,
            "unsorted": true
        },
        "totalElements": 2,
        "totalPages": 1
    }
}
```


### Sai tham số
```api
http://localhost:8080/api/v1/admin/users?keyword=Atmin&page=1&size=abc
```
#### Required
```json
{
    "timestamp": "2026-06-11T13:37:14",
    "status": 500,
    "error": "Internal Server Error",
    "message": "An unexpected error occurred on the server.",
    "path": "/api/v1/admin/users"
}
```
#### Expected
```json
{
  "timestamp": "2026-06-11T05:55:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Failed to convert value of type 'java.lang.String' to required type 'int' for parameter 'page' / 'size'",
  "path": "/api/v1/admin/users"
}
```

#### Update,Create User truyền đúng token access nhưng 
```json
{
    "timestamp": "2026-06-11T13:48:50",
    "status": 401,
    "error": "Unauthorized",
    "message": "Full authentication is required to access this resource",
    "path": "/error"
}
```
```json
{
  "timestamp": "2026-06-11T13:56:20",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred on the server.",
  "path": "/api/v1/admin/users/4"
}
```




