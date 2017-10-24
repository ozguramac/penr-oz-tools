Password generator module to help with producing deterministic password for each

1. Add jar to classpath and use it as a dependency to your application 
2. Look up secret in source code to construct a password generator instance or look into run(.bat or .sh)
3. Specify non-null, non-whitespace key and salt to generate method to get deterministic result
4. Note that you have to specify same salt across apps to get the same result
5. Example run takes in example.in and spits out example.in.result
