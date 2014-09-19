import scalariform.formatter.preferences._

scalariformSettings

ScalariformKeys.preferences := ScalariformKeys.preferences.value
  .setPreference(RewriteArrowSymbols, true)
  .setPreference(SpacesWithinPatternBinders, true)
  .setPreference(CompactControlReadability, false)
