URL examples

curl -X PATCH http://localhost:8000/api/teachers/1/ -H "Content-Type: application/json" -d '{"faculty": "True"}'
curl -X PATCH http://localhost:8000/api/teachers/3/ -H "Content-Type: application/json" -d '{"email": "akeen@calpoly.edu"}'

curl -X PUT http://localhost:8000/api/teachers/1/ -H "Content-Type: application/json" -d '{"canon": "eman", "non_canon": "edog"}'


curl http://localhost:8000/api/teachers/1/


curl -X DELETE http://localhost:8000/api/teachers/1/


curl -X POST http://localhost:8000/api/teachers/file/   -F "file=@faculty_names_use.xlsx"


curl -X POST http://localhost:8000/api/teachers/create/ -H "Content-Type: application/json" -d '{"canon": "newUser", "non_canon": "ghost"}'
