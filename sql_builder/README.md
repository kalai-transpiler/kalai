## Notes:

Divergences of sql_builder input syntax from Honey SQL syntax:

* Use strings instead of keywords
* Quote your own string literals
* FROM clause table name values should match the SELECT clause column values.
In other words, use a vector inside the vector when aliasing a table name, just as you would for aliasing column names.
  