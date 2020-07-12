# The pass pipeline

Each namespace in this directory peforms an sexpression rewrite except for the first and last pass.
The first step (maybe shouldn't be in pass?) takes a filename so that require dependencies can be analyzed.
The last step produces a string (maybe shouldn't be in pass?).
