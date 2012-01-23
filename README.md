# IClojure

An Interactive Clojure repl, inspired by IPython.

## License

Eclipse Public License (EPL), same as Clojure.

## Getting started

    wget https://github.com/downloads/cosmin/IClojure/iclojure-1.0.1-20120123-41.jar
    java -jar iclojure-1.0.1-20120123-41.jar

## Development

    git checkout https://github.com/cosmin/IClojure
    cd IClojure
    ./run.sh

## Package

    mvn clean package

## Features

- Tab completion, based on http://github.com/ninjudd/clojure-complete, itself based on swank-clojure.
- Shorthand for source and doc
- Shorthand for introspecting Java objects and classes via reflection
- Proper Control-C handling, although not very portable
- persist history across sessions to ~/.iclojure_history

## Tab completion

- variable
- method invocations
- "(.method" completion for all java methods for any of the classes in the current namespace
- "(. object method" completion for all the methods of the object (or a form that evaluates to an object)
- namespaces
- java classes

## Other shorthands

    ?symbol    => (doc symbol)
    ??symbol   => (source symbol)
    %d symbol  => show constructors, methods and fields of the given object or Class

## Roadmap

- input caching, similar to IPython
- output caching, similar to IPython
- tab completion for import, require and use forms
- abort long runing tasks with Ctrl+C
- better stack traces
- find class by name (to know what to import)
