/**
 * Sample React Native App
 * https://github.com/facebook/react-native
 *
 * @format
 * @flow
 */

import React, {useRef, useState} from 'react';
import {Button} from 'react-native';
import MPVPlayerView from './MPVPlayerView';

const App: () => React$Node = () => {
  const ref = useRef(null);

  const [seek, setSeek] = useState(0);
  const [play, setPlay] = useState(true);
  const [stop, setStop] = useState(false);
  const [mute, setMute] = useState(false);
  const [onPause, setOnPause] = useState(false);
  const [pos, setPos] = useState({currentTime: 0, duration: -1});

  return (
    <>
      <Button onPress={() => setPlay(!play)} title={`play=${play}`} />
      <Button onPress={() => setStop(!stop)} title={`stop=${stop}`} />
      <Button onPress={() => setMute(!mute)} title={`mute=${mute}`} />
      <Button onPress={() => setSeek(0)} title="Seek to 0:0" />
      <Button onPress={() => setSeek(15)} title="Seek to 0:15" />
      <Button onPress={() => setSeek(30)} title="Seek to 0:30" />
      <Button onPress={() => setOnPause(!onPause)} title={`onPause=${onPause}`} />
      <Button title={`pos=${JSON.stringify(pos)}`} />
      <MPVPlayerView
        ref={ref}
        seek={seek}
        play={play}
        mute={mute}
        stop={stop}
        onPause={onPause}
        style={{flex: 1, width: '100%', height: '100%'}}
        url="http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        onLoad={event => {
          console.log('almond-load', event.nativeEvent);
        }}
        onProgress={event => {
          console.log('almond-progress', event.nativeEvent);
          setPos(event.nativeEvent);
        }}
        onEnd={event => {
          console.log('almond-end', event.nativeEvent);
        }}
        onError={event => {
          console.log('almond-error', event.nativeEvent);
        }}
        onBuffer={event => {
          console.log('almond-buffer', event.nativeEvent);
        }}
      />
    </>
  );
};

export default App;
