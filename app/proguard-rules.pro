# SUBZERO R8 rules.
# Hilt, Room, Compose and kotlinx.serialization ship their own consumer rules;
# add app-specific keep rules here only with a comment explaining why.

# Keep line numbers so crash stack traces map back to source (with the retained mapping.txt);
# hide the original file name to avoid leaking module structure.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
