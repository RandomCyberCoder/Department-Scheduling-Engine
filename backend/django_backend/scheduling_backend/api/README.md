URL examples

curl -X PATCH http://localhost:8000/api/teachers/1/ -H "Content-Type: application/json" -d '{"faculty": "True"}'


curl -X PUT http://localhost:8000/api/teachers/1/ -H "Content-Type: application/json" -d '{"canon": "eman", "non_canon": "edog"}'


curl http://localhost:8000/api/teachers/1/

curl http://localhost:8000/api/teachers/?department=csc


curl -X DELETE http://localhost:8000/api/teachers/1/


curl -X POST http://localhost:8000/api/teachers/file/   -F "file=@faculty_names_use.xlsx"


curl -X POST http://localhost:8000/api/teachers/create/ -H "Content-Type: application/json" -d '{"canon": "newUser", "non_canon": "ghost"}'


curl -X PATCH http://localhost:8000/api/teachers/update/bulk/ -H "Content-Type: application/json" -d '[{"lookup": {"pk": 5}, "update_data": {"csc": "True"}}, {"lookup": {"non_canon_name": "bad", "canon_name": "Migler, Andrew Carl"}, "update_data": {"csc": "True"}}]'