import React, { useState } from 'react';
import { ActivityIndicator, SafeAreaView, ScrollView, StyleSheet, Text, TextInput, TouchableOpacity, View } from 'react-native';
import { StatusBar } from 'expo-status-bar';
import api from './api';

export default function App() {
  const [backendUrl, setBackendUrl] = useState(process.env.EXPO_PUBLIC_BACKEND_URL || 'http://10.0.2.2:9080');
  const [username, setUsername] = useState(process.env.EXPO_PUBLIC_TRANSLATOR_USERNAME || '');
  const [password, setPassword] = useState(process.env.EXPO_PUBLIC_TRANSLATOR_PASSWORD || '');
  const [text, setText] = useState('');
  const [translation, setTranslation] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function onTranslate() {
    setLoading(true);
    setError('');
    setTranslation('');
    try {
      setTranslation(await api.requestTranslation({ backendUrl, username, password, text }));
    } catch (requestError) {
      setError(requestError.message || 'Translation failed.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <SafeAreaView style={styles.safe}>
      <StatusBar style="dark" />
      <ScrollView contentContainerStyle={styles.container} keyboardShouldPersistTaps="handled">
        <Text style={styles.title}>Darija Translator</Text>
        <Text style={styles.label}>Backend URL</Text>
        <TextInput style={styles.input} value={backendUrl} onChangeText={setBackendUrl} autoCapitalize="none" keyboardType="url" />
        <Text style={styles.label}>Username</Text>
        <TextInput style={styles.input} value={username} onChangeText={setUsername} autoCapitalize="none" />
        <Text style={styles.label}>Password</Text>
        <TextInput style={styles.input} value={password} onChangeText={setPassword} secureTextEntry />
        <Text style={styles.label}>Text to translate</Text>
        <TextInput style={[styles.input, styles.multiline]} value={text} onChangeText={setText} multiline placeholder="Enter text in any language" />
        <TouchableOpacity style={[styles.button, loading && styles.disabled]} onPress={onTranslate} disabled={loading}>
          <Text style={styles.buttonText}>{loading ? 'Translating…' : 'Translate to Darija'}</Text>
        </TouchableOpacity>
        {loading && <ActivityIndicator style={styles.feedback} color="#176b4d" />}
        {!!error && <Text style={[styles.feedback, styles.error]} accessibilityRole="alert">{error}</Text>}
        {!!translation && <View style={styles.result}><Text style={styles.resultText}>{translation}</Text></View>}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: '#f7f5ef' },
  container: { padding: 22, gap: 8 },
  title: { fontSize: 30, fontWeight: '800', color: '#153b2d', marginBottom: 12 },
  label: { fontWeight: '700', color: '#263a33', marginTop: 8 },
  input: { backgroundColor: 'white', borderColor: '#8aa096', borderWidth: 1, borderRadius: 8, padding: 12 },
  multiline: { minHeight: 130, textAlignVertical: 'top' },
  button: { marginTop: 12, backgroundColor: '#176b4d', padding: 15, borderRadius: 8, alignItems: 'center' },
  disabled: { opacity: 0.55 },
  buttonText: { color: 'white', fontWeight: '800' },
  feedback: { marginTop: 14 },
  error: { color: '#9b231b', backgroundColor: '#ffe8e6', padding: 12, borderRadius: 8 },
  result: { marginTop: 14, backgroundColor: 'white', borderLeftColor: '#176b4d', borderLeftWidth: 5, borderRadius: 8, padding: 16 },
  resultText: { fontSize: 21, textAlign: 'right', writingDirection: 'rtl', color: '#182923' },
});
