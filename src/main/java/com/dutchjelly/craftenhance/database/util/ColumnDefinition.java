package com.dutchjelly.craftenhance.database.util;

public class ColumnDefinition {
        private final String name;
        private final String definition;

        public ColumnDefinition(String name, String definition) {
            this.name = name;
            this.definition = definition;
        }

        public String getName() {
            return name;
        }

        public String getDefinition() {
            return definition;
        }
    }