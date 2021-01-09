import {requireNativeComponent, ViewPropTypes} from 'react-native';
import PropTypes from 'prop-types';

var viewProps = {
  name: 'MPVPlayerView',
  propTypes: {
    url: PropTypes.string,
    ...ViewPropTypes,
  },
};
module.exports = requireNativeComponent('MPVPlayerView', viewProps);
