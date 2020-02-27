module.exports = {
  env: {
    browser: true,
    es6: true
  },
  plugins: [
    'cypress'
  ],
  extends: [
    "plugin:cypress/recommended"
  ],
  parserOptions: {
    ecmaVersion: 2018,
    sourceType: 'module'
  },
  rules: {
    "semi": ["error", "always"],
    "comma-dangle": ["error", "always-multiline"],
  }
};
