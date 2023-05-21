## Notes:

Divergences of sql_builder input syntax from Honey SQL syntax:

* Use strings instead of keywords
* Quote your own string literals
* FROM clause table name values should match the SELECT clause column values.
In other words, use a vector inside the vector when aliasing a table name, just as you would for aliasing column names.
  
## Parameterized Queries and Literals

We handle parameterization fundamentally differently from HoneySQL. 
We do so intentionally in order to leave parameterization completely up to the user.

We represent table names, column names, operators, and other non-numeric literals as strings.
This differs from HoneySQL, which uses keywords for table names, column names, operators -- basically, things that are not strings or numbers.
One reason for our representation is because not all downstream languages can support keywords.
In addition, it also follows SQL conventions more closely.

```clojure
{:select ["a" "b" "c"]
 :from   ["foo"]}
```

This means that if you want to provide an embedded string literal, you must include the quotes within your string.

```clojure
{:select ["a" "b" "c"]
 :from   ["foo"]
 :where  ["=" "foo.a" "'baz'"]}
```

This allows users to create their own parameterized queries directly. They can achieve this by using a non-quoted `?` in their query.

```clojure
{:select ["a" "b" "c"]
 :from   ["foo"]
 :where  ["=" "foo.a" "?"]}
```

Users should avoid quoting strings from external sources because this allows SQL injection.
Using a parameter avoids SQL injection, so that is the preferred way to include values from an external source.

The following image illustrates how SQL injection attacks could occur. For a more thorough explanation, [see here](https://www.explainxkcd.com/wiki/index.php/Little_Bobby_Tables).

![See Explain XKCD article titled "Little Bobby Tables"](https://imgs.xkcd.com/comics/exploits_of_a_mom.png)

Therefore, for queries that use variables, you should construct those queries using the `?` parameter syntax. 
As a consequence, values from external sources will always be used safely by the DB/SQL client library.

### Named Parameters

Currently, we do not support named parameters. 
The idea of named parameters would be to provide names to parameters in the query, and provide a map of values for those names along with the query.
This would allow repeated use of the same value and create explicit naming.
They are not a standardized feature of SQL.
Named parameters are not as useful as they sound.
Developers would already be using a named variable in their code.

If we wanted, we could create an algorithm that converts a formatted SQL query string into a query string with named parameters. Algorithm:
- take the formatted SQL string
- find occurrences of special names (where "special" could mean beginning with "?" or ":" character) to create an ordering of them
- use the map of name->value substitution values to construct the sequence of values that correspond to their named parameters according to their occurences
- replace the special names with `?`
