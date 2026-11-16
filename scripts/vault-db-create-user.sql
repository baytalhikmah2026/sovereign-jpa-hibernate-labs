CREATE ROLE "{{name}}" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}' IN ROLE sovereign_app;
ALTER ROLE "{{name}}" SET role = 'sovereign_app';
