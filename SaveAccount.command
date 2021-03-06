#!/bin/bash

dirname=`dirname $0`


echo "buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath \"nu.studer:gradle-credentials-plugin:1.0.1\"
    }
}

apply plugin: 'nu.studer.credentials'" > /tmp/script

echo -n "Input your git username : "

read username

echo "username=$username" >> ~/.gradle/gradle.properties 

echo -n "Input your git password : "

stty_save=`stty -g`
trap 'stty "$stty_save";' EXIT
trap 'exit_loop=true; exit 10' HUP INT QUIT ABRT TERM  # how to kill the read?
stty cbreak -echo </dev/tty >/dev/tty 2>&1  # raw mode

# Prompt and read password one character at a time
while IFS= read -r -n1 char; do
  # Was the read Interupted - not working yet
  [ "$exit_loop" -o $? -ne 0 ] && { code=03; break; }

  # Hexadecimal Code of the users key press...
  code="$( echo -n "$char" | od -An -tx1 | tr -d ' ' )"
  case "$code" in
  ''|03|0a|0d) break ;;               # Finish on EOF, ^C, LineFeed, or Return
  08|7f)                              # Backspace or Delete
      if [ -n "$PWORD" ]; then
        PWORD="$( echo "$PWORD" | sed 's/.$//' )"
        echo -n $'\b \b' >/dev/tty
      fi
      ;;
  15)                                 # ^U or kill line
      ç
      #echo -n "$PWORD" | tr -c ' '  ' '  >/dev/tty  # clear stars
      #echo -n "$PWORD" | tr -c '\b' '\b' >/dev/tty  # backspace
      echo -n "$PWORD" | sed 's/./\cH \cH/g' >/dev/tty # remove in reverse
      PWORD=''
      ;;
  [01]?) ;;                           # Ignore ALL other control characters
  *)  PWORD="$PWORD$char"             # Normal char - record it
      echo -n '*' >/dev/tty
      ;;
  esac
done

# Disable TTY cbreak mode
stty "$stty_save"
trap - EXIT HUP INT QUIT TERM

case "$code" in   # what was the last code - and report appropriatally
  03) echo >/dev/tty "==== INTERPUT ====" ;;   # currently does not happen
  *)  echo >/dev/tty ;
esac

$dirname/gradlew -b/tmp/script addCredentials --key $username --value $PWORD

